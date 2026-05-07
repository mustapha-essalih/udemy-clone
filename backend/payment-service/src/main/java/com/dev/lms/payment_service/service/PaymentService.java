package com.dev.lms.payment_service.service;

import com.dev.lms.payment_service.client.CourseClient;
import com.dev.lms.payment_service.dto.CheckoutRequest;
import com.dev.lms.payment_service.dto.CheckoutResponse;
import com.dev.lms.payment_service.dto.CourseInfoDto;
import com.dev.lms.payment_service.dto.PaymentResponse;
import com.dev.lms.payment_service.entity.Enrollment;
import com.dev.lms.payment_service.entity.Payment;
import com.dev.lms.payment_service.entity.PaymentStatus;
import com.dev.lms.payment_service.event.EnrollmentCreatedEvent;
import com.dev.lms.payment_service.exception.BusinessException;
import com.dev.lms.payment_service.kafka.EnrollmentEventPublisher;
import com.dev.lms.payment_service.repository.EnrollmentRepository;
import com.dev.lms.payment_service.repository.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseClient courseClient;
    private final EnrollmentEventPublisher eventPublisher;

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @Value("${stripe.success-url}")
    private String successUrl;

    @Value("${stripe.cancel-url}")
    private String cancelUrl;

    @PostConstruct
    void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    @Transactional
    public CheckoutResponse createCheckoutSession(CheckoutRequest request, UUID userId, String userIdStr) {
        if (enrollmentRepository.existsByUserIdAndCourseId(userId, request.courseId())) {
            throw new BusinessException("Already enrolled in this course");
        }

        String idempotencyKey = userId + ":" + request.courseId();
        paymentRepository.findByIdempotencyKey(idempotencyKey).ifPresent(existing -> {
            if (existing.getStatus() == PaymentStatus.PENDING) {
                throw new BusinessException("A pending payment already exists for this course");
            }
        });

        CourseInfoDto course = courseClient.getCourseInfo(request.courseId(), userIdStr);

        if (!"PUBLISHED".equals(course.status())) {
            throw new BusinessException("Course is not available for purchase");
        }

        BigDecimal price = course.isFree() ? BigDecimal.ZERO : course.price();
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Invalid course price");
        }

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(cancelUrl)
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency("usd")
                                    .setUnitAmount(price.multiply(BigDecimal.valueOf(100)).longValue())
                                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName(course.title())
                                            .build())
                                    .build())
                            .build())
                    .putMetadata("userId", userId.toString())
                    .putMetadata("courseId", request.courseId().toString())
                    .putMetadata("instructorId", course.instructorId().toString())
                    .build();

            Session session = Session.create(params);

            Payment payment = Payment.builder()
                    .userId(userId)
                    .courseId(request.courseId())
                    .amount(price)
                    .currency("usd")
                    .status(PaymentStatus.PENDING)
                    .stripeSessionId(session.getId())
                    .idempotencyKey(idempotencyKey)
                    .build();

            paymentRepository.save(payment);

            return new CheckoutResponse(payment.getId(), session.getId(), session.getUrl());

        } catch (StripeException e) {
            log.error("Stripe checkout session creation failed: {}", e.getMessage());
            throw new BusinessException("Payment initialization failed");
        }
    }

    @Transactional
    public void handleWebhook(byte[] payload, String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(new String(payload), sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Stripe webhook signature verification failed");
            throw new BusinessException("Invalid webhook signature");
        }

        if (!"checkout.session.completed".equals(event.getType())) {
            return;
        }

        Session session = (Session) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new BusinessException("Failed to deserialize Stripe event"));

        paymentRepository.findByStripeSessionId(session.getId()).ifPresent(payment -> {
            if (payment.getStatus() != PaymentStatus.PENDING) {
                return;
            }

            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setStripePaymentIntentId(session.getPaymentIntent());
            paymentRepository.save(payment);

            UUID userId = UUID.fromString(session.getMetadata().get("userId"));
            UUID courseId = UUID.fromString(session.getMetadata().get("courseId"));
            UUID instructorId = UUID.fromString(session.getMetadata().get("instructorId"));

            if (!enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
                Enrollment enrollment = Enrollment.builder()
                        .userId(userId)
                        .courseId(courseId)
                        .paymentId(payment.getId())
                        .build();
                enrollment = enrollmentRepository.save(enrollment);

                eventPublisher.publishEnrollmentCreated(new EnrollmentCreatedEvent(
                        enrollment.getId(),
                        userId,
                        courseId,
                        instructorId,
                        enrollment.getEnrolledAt()
                ));

                log.info("Enrollment created for user {} in course {}", userId, courseId);
            }
        });
    }

    public List<PaymentResponse> getPaymentsByUser(UUID userId) {
        return paymentRepository.findByUserId(userId).stream()
                .map(p -> new PaymentResponse(p.getId(), p.getCourseId(), p.getAmount(),
                        p.getCurrency(), p.getStatus(), p.getCreatedAt()))
                .toList();
    }
}

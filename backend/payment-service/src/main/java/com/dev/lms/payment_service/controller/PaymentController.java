package com.dev.lms.payment_service.controller;

import com.dev.lms.common.response.ApiResponse;
import com.dev.lms.payment_service.dto.CheckoutRequest;
import com.dev.lms.payment_service.dto.CheckoutResponse;
import com.dev.lms.payment_service.dto.PaymentResponse;
import com.dev.lms.payment_service.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/checkout")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<CheckoutResponse>> checkout(
            @Valid @RequestBody CheckoutRequest request,
            @RequestHeader("X-User-Id") String userId) {
        UUID userUuid = UUID.fromString(userId);
        CheckoutResponse response = paymentService.createCheckoutSession(request, userUuid, userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/stripe/webhook")
    public ResponseEntity<Void> stripeWebhook(
            HttpServletRequest request,
            @RequestHeader("Stripe-Signature") String sigHeader) throws IOException {
        byte[] payload = request.getInputStream().readAllBytes();
        paymentService.handleWebhook(payload, sigHeader);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getMyPayments(
            @RequestHeader("X-User-Id") String userId) {
        List<PaymentResponse> payments = paymentService.getPaymentsByUser(UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.ok(payments));
    }
}

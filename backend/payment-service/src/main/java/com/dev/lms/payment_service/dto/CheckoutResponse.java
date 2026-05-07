package com.dev.lms.payment_service.dto;

import java.util.UUID;

public record CheckoutResponse(
        UUID paymentId,
        String sessionId,
        String checkoutUrl
) {}

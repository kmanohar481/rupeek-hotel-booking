package com.rupeek.hotel.payment.controller;

import com.rupeek.hotel.common.constants.AppConstants;
import com.rupeek.hotel.payment.dto.PaymentRequest;
import com.rupeek.hotel.payment.dto.PaymentResponse;
import com.rupeek.hotel.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * A declined payment is a normal outcome (200 with paymentStatus=FAILED), not an error.
     * Retrying with the same Idempotency-Key returns the original result without charging again.
     */
    @PostMapping("/api/bookings/{bookingId}/payments")
    public PaymentResponse pay(@PathVariable String bookingId,
                               @RequestHeader(AppConstants.IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
                               @Valid @RequestBody PaymentRequest request) {
        return PaymentResponse.from(paymentService.pay(bookingId, idempotencyKey, request));
    }
}

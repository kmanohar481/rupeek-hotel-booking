package com.rupeek.hotel.payment.dto;

import com.rupeek.hotel.payment.constants.PaymentMethod;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Method plus method-specific details, e.g. {"cardNumber": "..."} for CARD or {"vpa": "..."} for UPI.
 * The amount is never taken from the client - it always comes from the booking.
 */
public record PaymentRequest(@NotNull PaymentMethod method, Map<String, String> details) {

    public PaymentRequest {
        details = details == null ? Map.of() : Map.copyOf(details);
    }
}

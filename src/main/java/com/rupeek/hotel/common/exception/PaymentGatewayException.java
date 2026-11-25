package com.rupeek.hotel.common.exception;

/**
 * The payment gateway could not complete an operation. Mapped to 502.
 */
public class PaymentGatewayException extends RuntimeException {

    public PaymentGatewayException(String message) {
        super(message);
    }
}

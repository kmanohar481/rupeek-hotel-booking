package com.rupeek.hotel.payment.model;

import com.rupeek.hotel.common.model.Money;
import com.rupeek.hotel.payment.constants.PaymentMethod;
import com.rupeek.hotel.payment.constants.PaymentStatus;

/**
 * One payment attempt for a booking, stored under the client's idempotency key.
 */
public record Payment(
        String id,
        String bookingId,
        String idempotencyKey,
        PaymentMethod method,
        Money amount,
        PaymentStatus status,
        String gatewayReference,
        String message,
        Money refundedAmount) {

    public boolean isSuccessful() {
        return status == PaymentStatus.SUCCESS;
    }

    public Payment withRefund(Money refund) {
        return new Payment(id, bookingId, idempotencyKey, method, amount, status, gatewayReference, message,
                refundedAmount.plus(refund));
    }
}

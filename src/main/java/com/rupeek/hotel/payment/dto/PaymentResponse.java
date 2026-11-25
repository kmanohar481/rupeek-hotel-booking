package com.rupeek.hotel.payment.dto;

import com.rupeek.hotel.booking.constants.BookingStatus;
import com.rupeek.hotel.common.model.Money;
import com.rupeek.hotel.payment.constants.PaymentMethod;
import com.rupeek.hotel.payment.constants.PaymentStatus;
import com.rupeek.hotel.payment.model.Payment;

public record PaymentResponse(
        String paymentId,
        String bookingId,
        PaymentMethod method,
        Money amount,
        PaymentStatus paymentStatus,
        String message,
        BookingStatus bookingStatus) {

    public static PaymentResponse from(PaymentResult result) {
        Payment payment = result.payment();
        return new PaymentResponse(payment.id(), payment.bookingId(), payment.method(), payment.amount(),
                payment.status(), payment.message(), result.bookingStatus());
    }
}

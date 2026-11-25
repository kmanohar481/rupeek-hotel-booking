package com.rupeek.hotel.payment.repository;

import com.rupeek.hotel.payment.model.Payment;

import java.util.Optional;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findByBookingIdAndIdempotencyKey(String bookingId, String idempotencyKey);

    Optional<Payment> findSuccessfulPayment(String bookingId);
}

package com.rupeek.hotel.payment.repository;

import com.rupeek.hotel.common.utils.InMemoryRepository;
import com.rupeek.hotel.payment.model.Payment;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class InMemoryPaymentRepository extends InMemoryRepository<Payment> implements PaymentRepository {

    public InMemoryPaymentRepository() {
        super(Payment::id);
    }

    @Override
    public Optional<Payment> findByBookingIdAndIdempotencyKey(String bookingId, String idempotencyKey) {
        return stream()
                .filter(payment -> payment.bookingId().equals(bookingId))
                .filter(payment -> payment.idempotencyKey().equals(idempotencyKey))
                .findFirst();
    }

    @Override
    public Optional<Payment> findSuccessfulPayment(String bookingId) {
        return stream()
                .filter(payment -> payment.bookingId().equals(bookingId))
                .filter(Payment::isSuccessful)
                .findFirst();
    }
}

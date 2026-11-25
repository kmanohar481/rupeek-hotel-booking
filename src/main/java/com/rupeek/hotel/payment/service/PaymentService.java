package com.rupeek.hotel.payment.service;

import com.rupeek.hotel.booking.model.Booking;
import com.rupeek.hotel.booking.service.BookingService;
import com.rupeek.hotel.common.constants.AppConstants;
import com.rupeek.hotel.common.exception.InvalidRequestException;
import com.rupeek.hotel.common.exception.NotFoundException;
import com.rupeek.hotel.common.exception.PaymentGatewayException;
import com.rupeek.hotel.common.model.Money;
import com.rupeek.hotel.payment.constants.PaymentMethod;
import com.rupeek.hotel.payment.constants.PaymentStatus;
import com.rupeek.hotel.payment.dto.PaymentRequest;
import com.rupeek.hotel.payment.dto.PaymentResult;
import com.rupeek.hotel.payment.gateway.GatewayResponse;
import com.rupeek.hotel.payment.gateway.PaymentGateway;
import com.rupeek.hotel.payment.model.Payment;
import com.rupeek.hotel.payment.repository.PaymentRepository;
import com.rupeek.hotel.payment.service.processor.PaymentProcessor;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    private final BookingService bookingService;
    private final PaymentRepository paymentRepository;
    private final PaymentGateway gateway;
    private final Map<PaymentMethod, PaymentProcessor> processors = new EnumMap<>(PaymentMethod.class);

    public PaymentService(BookingService bookingService,
                          PaymentRepository paymentRepository,
                          PaymentGateway gateway,
                          List<PaymentProcessor> processors) {
        this.bookingService = bookingService;
        this.paymentRepository = paymentRepository;
        this.gateway = gateway;
        processors.forEach(processor -> this.processors.put(processor.method(), processor));
    }

    /**
     * Pays for a booking. The outcome drives the booking: success confirms it, failure releases the room.
     * <p>
     * Runs under the booking lock, so a retry with the same idempotency key always sees the first attempt
     * and gets the same result back instead of charging twice.
     */
    public PaymentResult pay(String bookingId, String idempotencyKey, PaymentRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new InvalidRequestException(AppConstants.IDEMPOTENCY_KEY_HEADER + " is required");
        }
        return bookingService.withLockedBooking(bookingId, booking -> {
            Payment previous = paymentRepository.findByBookingIdAndIdempotencyKey(bookingId, idempotencyKey)
                    .orElse(null);
            if (previous != null) {
                return new PaymentResult(previous, booking.status());
            }

            booking.requirePayable();
            GatewayResponse response = processorFor(request.method()).charge(booking.totalAmount(), request.details());
            Payment payment = paymentRepository.save(toPayment(booking, idempotencyKey, request.method(), response));
            applyOutcome(booking, payment);
            return new PaymentResult(payment, booking.status());
        });
    }

    /**
     * Refunds part or all of a booking's successful payment. Callers must hold the booking lock.
     */
    public Payment refund(String bookingId, Money amount) {
        Payment payment = paymentRepository.findSuccessfulPayment(bookingId)
                .orElseThrow(() -> new NotFoundException("No successful payment for booking " + bookingId));
        GatewayResponse response = gateway.refund(payment.gatewayReference(), amount);
        if (!response.success()) {
            throw new PaymentGatewayException("Refund failed for booking " + bookingId + ": " + response.message());
        }
        return paymentRepository.save(payment.withRefund(amount));
    }

    private void applyOutcome(Booking booking, Payment payment) {
        if (payment.isSuccessful()) {
            booking.confirm();
        } else {
            booking.markPaymentFailed();
            bookingService.releaseInventory(booking);
        }
    }

    private PaymentProcessor processorFor(PaymentMethod method) {
        PaymentProcessor processor = processors.get(method);
        if (processor == null) {
            throw new InvalidRequestException("Unsupported payment method: " + method);
        }
        return processor;
    }

    private Payment toPayment(Booking booking, String idempotencyKey, PaymentMethod method, GatewayResponse response) {
        PaymentStatus status = response.success() ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
        return new Payment(UUID.randomUUID().toString(), booking.id(), idempotencyKey, method,
                booking.totalAmount(), status, response.reference(), response.message(), Money.ZERO);
    }
}

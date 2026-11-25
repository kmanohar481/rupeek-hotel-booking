package com.rupeek.hotel.cancellation.service;

import com.rupeek.hotel.booking.constants.BookingStatus;
import com.rupeek.hotel.booking.model.Booking;
import com.rupeek.hotel.booking.service.BookingService;
import com.rupeek.hotel.cancellation.constants.CancellationPolicyType;
import com.rupeek.hotel.cancellation.dto.CancellationResult;
import com.rupeek.hotel.cancellation.service.policy.CancellationPolicy;
import com.rupeek.hotel.common.exception.InvalidRequestException;
import com.rupeek.hotel.common.model.Money;
import com.rupeek.hotel.payment.service.PaymentService;
import com.rupeek.hotel.property.service.PropertyService;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class CancellationService {

    private final BookingService bookingService;
    private final PropertyService propertyService;
    private final PaymentService paymentService;
    private final Clock clock;
    private final Map<CancellationPolicyType, CancellationPolicy> policies = new EnumMap<>(CancellationPolicyType.class);

    public CancellationService(BookingService bookingService,
                               PropertyService propertyService,
                               PaymentService paymentService,
                               Clock clock,
                               List<CancellationPolicy> policies) {
        this.bookingService = bookingService;
        this.propertyService = propertyService;
        this.paymentService = paymentService;
        this.clock = clock;
        policies.forEach(policy -> this.policies.put(policy.type(), policy));
    }

    /**
     * Cancels a booking, frees its room and refunds according to the property's policy.
     * An unpaid booking is simply cancelled - there is nothing to refund.
     */
    public CancellationResult cancel(String bookingId) {
        return bookingService.withLockedBooking(bookingId, booking -> {
            LocalDate today = LocalDate.now(clock);
            if (!today.isBefore(booking.stay().checkIn())) {
                throw new InvalidRequestException("Booking cannot be cancelled on or after the check-in date");
            }
            boolean wasPaid = booking.status() == BookingStatus.CONFIRMED;

            booking.cancel();
            bookingService.releaseInventory(booking);

            Money refund = wasPaid ? refundFor(booking, today) : Money.ZERO;
            if (refund.isPositive()) {
                paymentService.refund(booking.id(), refund);
            }
            return new CancellationResult(booking.id(), booking.status(), refund);
        });
    }

    private Money refundFor(Booking booking, LocalDate today) {
        CancellationPolicyType policyType = propertyService.getProperty(booking.propertyId()).cancellationPolicy();
        long daysBeforeCheckIn = ChronoUnit.DAYS.between(today, booking.stay().checkIn());
        return policies.get(policyType).refundFor(booking.totalAmount(), daysBeforeCheckIn);
    }
}

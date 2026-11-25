package com.rupeek.hotel.booking.model;

import com.rupeek.hotel.booking.constants.BookingStatus;
import com.rupeek.hotel.common.exception.InvalidStateTransitionException;
import com.rupeek.hotel.common.model.DateRange;
import com.rupeek.hotel.common.model.Money;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A guest's reservation of one room of a room type for a stay.
 * Everything except the status is fixed at creation, including the price, so later price changes
 * never affect an existing booking. Status changes happen under the booking lock (see BookingService).
 */
public class Booking {

    private final String id;
    private final String propertyId;
    private final String roomTypeId;
    private final String guestName;
    private final DateRange stay;
    private final int guests;
    private final Money totalAmount;
    private final Instant createdAt;
    private volatile BookingStatus status;

    public Booking(String id, String propertyId, String roomTypeId, String guestName,
                   DateRange stay, int guests, Money totalAmount, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.propertyId = Objects.requireNonNull(propertyId);
        this.roomTypeId = Objects.requireNonNull(roomTypeId);
        this.guestName = Objects.requireNonNull(guestName);
        this.stay = Objects.requireNonNull(stay);
        this.guests = guests;
        this.totalAmount = Objects.requireNonNull(totalAmount);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.status = BookingStatus.PENDING_PAYMENT;
    }

    public void confirm() {
        moveTo(BookingStatus.CONFIRMED);
    }

    public void markPaymentFailed() {
        moveTo(BookingStatus.PAYMENT_FAILED);
    }

    public void expire() {
        moveTo(BookingStatus.EXPIRED);
    }

    public void cancel() {
        moveTo(BookingStatus.CANCELLED);
    }

    /**
     * Fails fast (before charging the guest) if the booking cannot be paid for.
     */
    public void requirePayable() {
        if (!status.canMoveTo(BookingStatus.CONFIRMED)) {
            throw new InvalidStateTransitionException("Booking " + id + " is " + status + " and cannot be paid");
        }
    }

    public boolean isHoldExpired(Instant now, Duration holdDuration) {
        return status == BookingStatus.PENDING_PAYMENT && createdAt.plus(holdDuration).isBefore(now);
    }

    private void moveTo(BookingStatus target) {
        requireCanMoveTo(target);
        status = target;
    }

    private void requireCanMoveTo(BookingStatus target) {
        if (!status.canMoveTo(target)) {
            throw new InvalidStateTransitionException(
                    "Booking " + id + " cannot move from " + status + " to " + target);
        }
    }

    public String id() {
        return id;
    }

    public String propertyId() {
        return propertyId;
    }

    public String roomTypeId() {
        return roomTypeId;
    }

    public String guestName() {
        return guestName;
    }

    public DateRange stay() {
        return stay;
    }

    public int guests() {
        return guests;
    }

    public Money totalAmount() {
        return totalAmount;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public BookingStatus status() {
        return status;
    }
}

package com.rupeek.hotel.booking.constants;

import java.util.Set;

/**
 * Booking lifecycle:
 * <pre>
 * PENDING_PAYMENT --pay ok------> CONFIRMED --cancel--> CANCELLED
 *        |--------pay failed----> PAYMENT_FAILED
 *        |--------hold timeout--> EXPIRED
 *        |--------cancel--------> CANCELLED
 * </pre>
 * Inventory is held only while a booking is PENDING_PAYMENT or CONFIRMED.
 */
public enum BookingStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    PAYMENT_FAILED,
    EXPIRED,
    CANCELLED;

    public boolean canMoveTo(BookingStatus target) {
        return allowedNext().contains(target);
    }

    private Set<BookingStatus> allowedNext() {
        return switch (this) {
            case PENDING_PAYMENT -> Set.of(CONFIRMED, PAYMENT_FAILED, EXPIRED, CANCELLED);
            case CONFIRMED -> Set.of(CANCELLED);
            case PAYMENT_FAILED, EXPIRED, CANCELLED -> Set.of();
        };
    }
}

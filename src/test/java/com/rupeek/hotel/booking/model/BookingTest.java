package com.rupeek.hotel.booking.model;

import com.rupeek.hotel.booking.constants.BookingStatus;
import com.rupeek.hotel.common.exception.InvalidStateTransitionException;
import com.rupeek.hotel.common.model.DateRange;
import com.rupeek.hotel.common.model.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-05T10:00:00Z");

    private Booking newBooking() {
        LocalDate checkIn = LocalDate.of(2026, 1, 10);
        return new Booking("b1", "p1", "r1", "Guest One", new DateRange(checkIn, checkIn.plusDays(2)), 2,
                Money.of(4000), CREATED_AT);
    }

    @Test
    void startsAwaitingPayment() {
        assertThat(newBooking().status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
    }

    @Test
    void pendingBookingCanBeConfirmedThenCancelled() {
        Booking booking = newBooking();

        booking.confirm();
        assertThat(booking.status()).isEqualTo(BookingStatus.CONFIRMED);

        booking.cancel();
        assertThat(booking.status()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void pendingBookingCanFailPaymentOrExpireOrBeCancelled() {
        Booking failed = newBooking();
        failed.markPaymentFailed();
        Booking expired = newBooking();
        expired.expire();
        Booking cancelled = newBooking();
        cancelled.cancel();

        assertThat(failed.status()).isEqualTo(BookingStatus.PAYMENT_FAILED);
        assertThat(expired.status()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(cancelled.status()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void confirmedBookingCannotBePaidAgain() {
        Booking booking = newBooking();
        booking.confirm();

        assertThatThrownBy(booking::requirePayable).isInstanceOf(InvalidStateTransitionException.class);
        assertThatThrownBy(booking::confirm).isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void confirmedBookingCannotExpire() {
        Booking booking = newBooking();
        booking.confirm();

        assertThatThrownBy(booking::expire).isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void cancelledBookingCannotBePaidOrCancelledAgain() {
        Booking booking = newBooking();
        booking.cancel();

        assertThatThrownBy(booking::requirePayable).isInstanceOf(InvalidStateTransitionException.class);
        assertThatThrownBy(booking::cancel).isInstanceOf(InvalidStateTransitionException.class);
    }

    @ParameterizedTest
    @EnumSource(value = BookingStatus.class, names = {"PAYMENT_FAILED", "EXPIRED", "CANCELLED"})
    void terminalStatesAllowNoFurtherTransitions(BookingStatus terminal) {
        for (BookingStatus target : BookingStatus.values()) {
            assertThat(terminal.canMoveTo(target)).as(terminal + " -> " + target).isFalse();
        }
    }

    @Test
    void holdExpiresOnlyAfterHoldDurationAndOnlyWhileUnpaid() {
        Booking booking = newBooking();
        Duration hold = Duration.ofMinutes(15);

        assertThat(booking.isHoldExpired(CREATED_AT.plus(Duration.ofMinutes(14)), hold)).isFalse();
        assertThat(booking.isHoldExpired(CREATED_AT.plus(Duration.ofMinutes(16)), hold)).isTrue();

        booking.confirm();
        assertThat(booking.isHoldExpired(CREATED_AT.plus(Duration.ofHours(1)), hold)).isFalse();
    }
}

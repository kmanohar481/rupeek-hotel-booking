package com.rupeek.hotel.cancellation.service;

import com.rupeek.hotel.booking.constants.BookingStatus;
import com.rupeek.hotel.booking.model.Booking;
import com.rupeek.hotel.cancellation.constants.CancellationPolicyType;
import com.rupeek.hotel.cancellation.dto.CancellationResult;
import com.rupeek.hotel.common.exception.InvalidRequestException;
import com.rupeek.hotel.common.exception.InvalidStateTransitionException;
import com.rupeek.hotel.common.model.Money;
import com.rupeek.hotel.payment.model.Payment;
import com.rupeek.hotel.property.model.Property;
import com.rupeek.hotel.support.TestFixture;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.rupeek.hotel.support.TestFixture.DECLINED_CARD;
import static com.rupeek.hotel.support.TestFixture.day;
import static com.rupeek.hotel.support.TestFixture.hotel;
import static com.rupeek.hotel.support.TestFixture.room;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CancellationServiceTest {

    private final TestFixture fixture = new TestFixture();
    private final CancellationService cancellationService = fixture.cancellationService;
    private final Property flexibleHotel = fixture.simpleHotel(1);

    @Test
    void earlyCancellationOfPaidBookingRefundsInFullAndFreesTheRoom() {
        Booking booking = fixture.confirmedBooking(flexibleHotel, day(10), day(12));

        CancellationResult result = cancellationService.cancel(booking.id());

        assertThat(result.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(result.refundAmount()).isEqualTo(booking.totalAmount());
        assertThat(fixture.gateway.refunds()).isEqualTo(1);
        Payment payment = fixture.paymentRepository.findSuccessfulPayment(booking.id()).orElseThrow();
        assertThat(payment.refundedAmount()).isEqualTo(booking.totalAmount());
        assertThat(fixture.bookedRooms(flexibleHotel, day(10))).isZero();
    }

    @Test
    void lateCancellationGetsPartialRefund() {
        Booking booking = fixture.confirmedBooking(flexibleHotel, day(3), day(5)); // 4000 total

        assertThat(cancellationService.cancel(booking.id()).refundAmount()).isEqualTo(Money.of(2000));
    }

    @Test
    void nonRefundablePropertyRefundsNothingButStillFreesTheRoom() {
        Property strictHotel = fixture.onboard(hotel("Strict Stay", "Goa", "Baga", 3, Set.of(),
                CancellationPolicyType.NON_REFUNDABLE, room("Std", 2, 1, 1500)));
        Booking booking = fixture.confirmedBooking(strictHotel, day(20), day(21));

        CancellationResult result = cancellationService.cancel(booking.id());

        assertThat(result.refundAmount()).isEqualTo(Money.ZERO);
        assertThat(fixture.gateway.refunds()).isZero();
        assertThat(fixture.bookedRooms(strictHotel, day(20))).isZero();
    }

    @Test
    void unpaidBookingIsCancelledWithoutRefund() {
        Booking booking = fixture.book(flexibleHotel, day(10), day(12));

        CancellationResult result = cancellationService.cancel(booking.id());

        assertThat(result.refundAmount()).isEqualTo(Money.ZERO);
        assertThat(fixture.gateway.refunds()).isZero();
        assertThat(fixture.bookedRooms(flexibleHotel, day(10))).isZero();
    }

    @Test
    void cancellingTwiceIsRejectedAndRoomIsReleasedOnlyOnce() {
        Booking booking = fixture.confirmedBooking(flexibleHotel, day(10), day(12));
        cancellationService.cancel(booking.id());

        assertThatThrownBy(() -> cancellationService.cancel(booking.id()))
                .isInstanceOf(InvalidStateTransitionException.class);
        assertThat(fixture.bookedRooms(flexibleHotel, day(10))).isZero();
        assertThat(fixture.gateway.refunds()).isEqualTo(1);
    }

    @Test
    void failedPaymentBookingCannotBeCancelled() {
        Booking booking = fixture.book(flexibleHotel, day(10), day(12));
        fixture.payByCard(booking.id(), DECLINED_CARD);

        assertThatThrownBy(() -> cancellationService.cancel(booking.id()))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void cannotCancelOnCheckInDay() {
        Booking booking = fixture.confirmedBooking(flexibleHotel, day(0), day(2));

        assertThatThrownBy(() -> cancellationService.cancel(booking.id()))
                .isInstanceOf(InvalidRequestException.class);
        assertThat(booking.status()).isEqualTo(BookingStatus.CONFIRMED);
    }
}

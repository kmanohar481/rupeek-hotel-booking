package com.rupeek.hotel.booking.service;

import com.rupeek.hotel.booking.constants.BookingStatus;
import com.rupeek.hotel.booking.dto.NewBooking;
import com.rupeek.hotel.booking.model.Booking;
import com.rupeek.hotel.common.exception.InvalidRequestException;
import com.rupeek.hotel.common.exception.NotFoundException;
import com.rupeek.hotel.common.exception.RoomNotAvailableException;
import com.rupeek.hotel.common.model.Money;
import com.rupeek.hotel.property.model.Property;
import com.rupeek.hotel.support.TestFixture;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static com.rupeek.hotel.support.TestFixture.day;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingServiceTest {

    private final TestFixture fixture = new TestFixture();
    private final BookingService bookingService = fixture.bookingService;
    private final Property hotel = fixture.simpleHotel(1);

    @Test
    void createsBookingAwaitingPaymentWithPriceForEveryNight() {
        Booking booking = fixture.book(hotel, day(10), day(13));

        assertThat(booking.status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(booking.totalAmount()).isEqualTo(Money.of(3 * 2000));
        assertThat(bookingService.getBooking(booking.id())).isSameAs(booking);
    }

    @Test
    void holdsTheRoomForEveryNightOfTheStay() {
        fixture.book(hotel, day(10), day(13));

        assertThat(fixture.bookedRooms(hotel, day(10))).isEqualTo(1);
        assertThat(fixture.bookedRooms(hotel, day(12))).isEqualTo(1);
        assertThat(fixture.bookedRooms(hotel, day(13))).isZero();
    }

    @Test
    void preventsDoubleBookingOfOverlappingDates() {
        fixture.book(hotel, day(10), day(13));

        assertThatThrownBy(() -> fixture.book(hotel, day(12), day(14)))
                .isInstanceOf(RoomNotAvailableException.class);
    }

    @Test
    void allowsBackToBackStays() {
        fixture.book(hotel, day(10), day(13));

        assertThat(fixture.book(hotel, day(13), day(15)).status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
    }

    @Test
    void rejectsRoomTypeThatBelongsToAnotherProperty() {
        Property otherHotel = fixture.simpleHotel(5);
        String foreignRoomTypeId = otherHotel.roomTypes().get(0).id();

        assertThatThrownBy(() -> bookingService.createBooking(new NewBooking(hotel.id(), foreignRoomTypeId,
                "Guest One", day(10), day(11), 1)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void rejectsUnknownProperty() {
        assertThatThrownBy(() -> bookingService.createBooking(new NewBooking("missing", "r", "Guest One",
                day(10), day(11), 1)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void rejectsMoreGuestsThanTheRoomAllows() {
        assertThatThrownBy(() -> bookingService.createBooking(new NewBooking(hotel.id(),
                hotel.roomTypes().get(0).id(), "Guest One", day(10), day(11), 3)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("at most 2 guests");
    }

    @Test
    void rejectsCheckInInThePast() {
        assertThatThrownBy(() -> fixture.book(hotel, day(-1), day(2)))
                .isInstanceOf(InvalidRequestException.class);
        assertThat(fixture.bookedRooms(hotel, day(0))).isZero();
    }

    @Test
    void unpaidBookingExpiresAfterHoldAndFreesTheRoom() {
        Booking unpaid = fixture.book(hotel, day(10), day(12));

        fixture.clock.advance(TestFixture.HOLD_DURATION.plus(Duration.ofSeconds(1)));
        int expired = bookingService.expireUnpaidBookings();

        assertThat(expired).isEqualTo(1);
        assertThat(unpaid.status()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(fixture.bookedRooms(hotel, day(10))).isZero();
    }

    @Test
    void bookingsWithinHoldOrAlreadyPaidAreNotExpired() {
        Booking paid = fixture.confirmedBooking(hotel, day(10), day(12));
        fixture.clock.advance(Duration.ofMinutes(10));
        Booking recent = fixture.book(hotel, day(20), day(21));

        fixture.clock.advance(Duration.ofMinutes(10));
        bookingService.expireUnpaidBookings();

        assertThat(paid.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(recent.status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
    }
}

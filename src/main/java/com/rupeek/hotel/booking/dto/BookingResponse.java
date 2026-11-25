package com.rupeek.hotel.booking.dto;

import com.rupeek.hotel.booking.constants.BookingStatus;
import com.rupeek.hotel.booking.model.Booking;
import com.rupeek.hotel.common.model.Money;

import java.time.Instant;
import java.time.LocalDate;

public record BookingResponse(
        String bookingId,
        String propertyId,
        String roomTypeId,
        String guestName,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        Money totalAmount,
        BookingStatus status,
        Instant createdAt) {

    public static BookingResponse from(Booking booking) {
        return new BookingResponse(booking.id(), booking.propertyId(), booking.roomTypeId(), booking.guestName(),
                booking.stay().checkIn(), booking.stay().checkOut(), booking.guests(), booking.totalAmount(),
                booking.status(), booking.createdAt());
    }
}

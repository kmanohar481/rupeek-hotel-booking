package com.rupeek.hotel.booking.repository;

import com.rupeek.hotel.booking.constants.BookingStatus;
import com.rupeek.hotel.booking.model.Booking;

import java.util.List;
import java.util.Optional;

public interface BookingRepository {

    Booking save(Booking booking);

    Optional<Booking> findById(String bookingId);

    List<Booking> findByStatus(BookingStatus status);
}

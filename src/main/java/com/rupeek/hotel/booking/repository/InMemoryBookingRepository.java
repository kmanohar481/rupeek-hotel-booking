package com.rupeek.hotel.booking.repository;

import com.rupeek.hotel.booking.constants.BookingStatus;
import com.rupeek.hotel.booking.model.Booking;
import com.rupeek.hotel.common.utils.InMemoryRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class InMemoryBookingRepository extends InMemoryRepository<Booking> implements BookingRepository {

    public InMemoryBookingRepository() {
        super(Booking::id);
    }

    @Override
    public List<Booking> findByStatus(BookingStatus status) {
        return stream().filter(booking -> booking.status() == status).toList();
    }
}

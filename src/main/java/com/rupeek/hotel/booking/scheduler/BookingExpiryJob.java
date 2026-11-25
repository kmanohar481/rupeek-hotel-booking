package com.rupeek.hotel.booking.scheduler;

import com.rupeek.hotel.booking.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically frees rooms held by bookings that were never paid.
 */
@Component
public class BookingExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(BookingExpiryJob.class);

    private final BookingService bookingService;

    public BookingExpiryJob(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Scheduled(fixedDelayString = "${booking.expiry-check-interval-ms:60000}")
    public void expireUnpaidBookings() {
        int expired = bookingService.expireUnpaidBookings();
        if (expired > 0) {
            log.info("Expired {} unpaid booking(s)", expired);
        }
    }
}

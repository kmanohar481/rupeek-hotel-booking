package com.rupeek.hotel.common.model;

import com.rupeek.hotel.common.constants.AppConstants;
import com.rupeek.hotel.common.exception.InvalidRequestException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * A stay from check-in (inclusive) to check-out (exclusive).
 * Nights are the dates the room is actually occupied, so 10th -> 12th is two nights: 10th and 11th.
 */
public record DateRange(LocalDate checkIn, LocalDate checkOut) {

    public DateRange {
        if (checkIn == null || checkOut == null) {
            throw new InvalidRequestException("Check-in and check-out dates are required");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidRequestException("Check-out must be after check-in");
        }
    }

    public long numberOfNights() {
        return ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public List<LocalDate> nights() {
        return checkIn.datesUntil(checkOut).toList();
    }

    public boolean overlaps(DateRange other) {
        return checkIn.isBefore(other.checkOut) && other.checkIn.isBefore(checkOut);
    }

    /**
     * Rules for a stay that can be searched or booked as of {@code today}.
     */
    public void requireBookableOn(LocalDate today) {
        if (checkIn.isBefore(today)) {
            throw new InvalidRequestException("Check-in date cannot be in the past");
        }
        if (numberOfNights() > AppConstants.MAX_STAY_NIGHTS) {
            throw new InvalidRequestException("A stay cannot be longer than " + AppConstants.MAX_STAY_NIGHTS + " nights");
        }
    }

    @Override
    public String toString() {
        return checkIn + " to " + checkOut;
    }
}

package com.rupeek.hotel.cancellation.dto;

import com.rupeek.hotel.booking.constants.BookingStatus;
import com.rupeek.hotel.common.model.Money;

public record CancellationResult(String bookingId, BookingStatus status, Money refundAmount) {
}

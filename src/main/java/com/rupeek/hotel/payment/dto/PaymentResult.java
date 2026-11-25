package com.rupeek.hotel.payment.dto;

import com.rupeek.hotel.booking.constants.BookingStatus;
import com.rupeek.hotel.payment.model.Payment;

public record PaymentResult(Payment payment, BookingStatus bookingStatus) {
}

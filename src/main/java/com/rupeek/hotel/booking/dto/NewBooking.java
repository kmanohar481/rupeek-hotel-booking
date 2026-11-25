package com.rupeek.hotel.booking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record NewBooking(
        @NotBlank String propertyId,
        @NotBlank String roomTypeId,
        @NotBlank String guestName,
        @NotNull LocalDate checkIn,
        @NotNull LocalDate checkOut,
        @NotNull @Min(1) Integer guests) {
}

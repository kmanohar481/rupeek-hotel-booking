package com.rupeek.hotel.property.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record NewRoomType(
        @NotBlank String name,
        @NotNull Integer maxGuests,
        @NotNull Integer totalRooms,
        @NotNull BigDecimal pricePerNight) {
}

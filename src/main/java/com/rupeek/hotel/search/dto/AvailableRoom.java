package com.rupeek.hotel.search.dto;

import com.rupeek.hotel.common.model.Money;

public record AvailableRoom(String roomTypeId, String name, int maxGuests, Money pricePerNight, Money totalPrice) {
}

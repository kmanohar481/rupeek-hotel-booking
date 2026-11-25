package com.rupeek.hotel.property.model;

import com.rupeek.hotel.common.exception.InvalidRequestException;
import com.rupeek.hotel.common.model.Money;

/**
 * A category of identical rooms in a property, e.g. "Deluxe King". Inventory is tracked per room type.
 */
public record RoomType(String id, String name, int maxGuests, int totalRooms, Money pricePerNight) {

    public RoomType {
        if (maxGuests < 1) {
            throw new InvalidRequestException("Room type '" + name + "' must allow at least one guest");
        }
        if (totalRooms < 1) {
            throw new InvalidRequestException("Room type '" + name + "' must have at least one room");
        }
        if (pricePerNight == null || !pricePerNight.isPositive()) {
            throw new InvalidRequestException("Room type '" + name + "' must have a positive price");
        }
    }

    public boolean canAccommodate(int guests) {
        return guests <= maxGuests;
    }
}

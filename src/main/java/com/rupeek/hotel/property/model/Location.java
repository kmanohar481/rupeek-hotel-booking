package com.rupeek.hotel.property.model;

import com.rupeek.hotel.common.exception.InvalidRequestException;

public record Location(String city, String locality, String addressLine) {

    public Location {
        if (city == null || city.isBlank()) {
            throw new InvalidRequestException("City is required");
        }
        if (locality == null || locality.isBlank()) {
            throw new InvalidRequestException("Locality is required");
        }
    }
}

package com.rupeek.hotel.search.dto;

import com.rupeek.hotel.property.constants.Amenity;
import com.rupeek.hotel.property.constants.PropertyType;
import com.rupeek.hotel.property.model.Location;

import java.util.List;
import java.util.Set;

public record PropertySearchResult(
        String propertyId,
        String name,
        PropertyType type,
        Location location,
        int starRating,
        Set<Amenity> amenities,
        List<AvailableRoom> availableRooms) {
}

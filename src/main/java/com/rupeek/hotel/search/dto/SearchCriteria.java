package com.rupeek.hotel.search.dto;

import com.rupeek.hotel.common.exception.InvalidRequestException;
import com.rupeek.hotel.common.model.DateRange;
import com.rupeek.hotel.property.constants.Amenity;

import java.math.BigDecimal;
import java.util.Set;

/**
 * What a guest is looking for. Optional criteria are null when not specified.
 */
public record SearchCriteria(
        String city,
        String locality,
        DateRange stay,
        int guests,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Set<Amenity> amenities,
        Integer minStarRating) {

    public SearchCriteria {
        if (isBlank(city) && isBlank(locality)) {
            throw new InvalidRequestException("Either city or locality is required");
        }
        if (stay == null) {
            throw new InvalidRequestException("Check-in and check-out dates are required");
        }
        if (guests < 1) {
            throw new InvalidRequestException("At least one guest is required");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new InvalidRequestException("minPrice cannot be greater than maxPrice");
        }
        amenities = amenities == null ? Set.of() : Set.copyOf(amenities);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

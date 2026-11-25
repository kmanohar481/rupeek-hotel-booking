package com.rupeek.hotel.property.dto;

import com.rupeek.hotel.cancellation.constants.CancellationPolicyType;
import com.rupeek.hotel.property.constants.Amenity;
import com.rupeek.hotel.property.constants.PropertyType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Set;

/**
 * Details needed to onboard a property. Business rules (rating range, positive price...) are enforced by the
 * domain objects themselves; annotations here only check the request is complete.
 */
public record NewProperty(
        @NotBlank String name,
        @NotNull PropertyType type,
        @NotBlank String city,
        @NotBlank String locality,
        String addressLine,
        @NotNull Integer starRating,
        Set<Amenity> amenities,
        CancellationPolicyType cancellationPolicy,
        @NotEmpty List<@Valid NewRoomType> roomTypes) {
}

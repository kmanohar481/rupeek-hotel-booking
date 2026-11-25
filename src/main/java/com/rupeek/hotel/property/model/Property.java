package com.rupeek.hotel.property.model;

import com.rupeek.hotel.cancellation.constants.CancellationPolicyType;
import com.rupeek.hotel.common.exception.InvalidRequestException;
import com.rupeek.hotel.property.constants.Amenity;
import com.rupeek.hotel.property.constants.PropertyType;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A property listed on the platform. Immutable once onboarded, which keeps it safe to read from any thread.
 */
public record Property(
        String id,
        String ownerId,
        String name,
        PropertyType type,
        Location location,
        int starRating,
        Set<Amenity> amenities,
        CancellationPolicyType cancellationPolicy,
        List<RoomType> roomTypes) {

    public Property {
        if (starRating < 1 || starRating > 5) {
            throw new InvalidRequestException("Star rating must be between 1 and 5");
        }
        if (roomTypes == null || roomTypes.isEmpty()) {
            throw new InvalidRequestException("Property must have at least one room type");
        }
        requireUniqueRoomTypeNames(roomTypes);
        amenities = amenities == null ? Set.of() : Set.copyOf(amenities);
        roomTypes = List.copyOf(roomTypes);
    }

    public Optional<RoomType> findRoomType(String roomTypeId) {
        return roomTypes.stream().filter(roomType -> roomType.id().equals(roomTypeId)).findFirst();
    }

    public boolean hasAllAmenities(Set<Amenity> required) {
        return amenities.containsAll(required);
    }

    private static void requireUniqueRoomTypeNames(List<RoomType> roomTypes) {
        Set<String> names = new HashSet<>();
        for (RoomType roomType : roomTypes) {
            if (!names.add(roomType.name().toLowerCase())) {
                throw new InvalidRequestException("Duplicate room type name: " + roomType.name());
            }
        }
    }
}

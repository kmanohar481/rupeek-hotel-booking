package com.rupeek.hotel.property.service;

import com.rupeek.hotel.cancellation.constants.CancellationPolicyType;
import com.rupeek.hotel.common.exception.NotFoundException;
import com.rupeek.hotel.common.model.Money;
import com.rupeek.hotel.property.dto.NewOwner;
import com.rupeek.hotel.property.dto.NewProperty;
import com.rupeek.hotel.property.dto.NewRoomType;
import com.rupeek.hotel.property.dto.OwnerPortfolio;
import com.rupeek.hotel.property.model.Location;
import com.rupeek.hotel.property.model.Owner;
import com.rupeek.hotel.property.model.Property;
import com.rupeek.hotel.property.model.RoomType;
import com.rupeek.hotel.property.repository.OwnerRepository;
import com.rupeek.hotel.property.repository.PropertyRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PropertyService {

    private final OwnerRepository ownerRepository;
    private final PropertyRepository propertyRepository;

    public PropertyService(OwnerRepository ownerRepository, PropertyRepository propertyRepository) {
        this.ownerRepository = ownerRepository;
        this.propertyRepository = propertyRepository;
    }

    /**
     * Same path for a standalone property (list of one) and a chain (list of many).
     * Every property is built - and therefore validated - before anything is saved.
     */
    public OwnerPortfolio onboardOwner(NewOwner request) {
        Owner owner = new Owner(newId(), request.name(), request.email());
        List<Property> properties = request.properties().stream()
                .map(newProperty -> toProperty(owner.id(), newProperty))
                .toList();

        ownerRepository.save(owner);
        properties.forEach(propertyRepository::save);
        return new OwnerPortfolio(owner, properties);
    }

    public Property addProperty(String ownerId, NewProperty request) {
        Owner owner = getOwner(ownerId);
        return propertyRepository.save(toProperty(owner.id(), request));
    }

    public OwnerPortfolio getPortfolio(String ownerId) {
        Owner owner = getOwner(ownerId);
        return new OwnerPortfolio(owner, propertyRepository.findByOwnerId(ownerId));
    }

    public Property getProperty(String propertyId) {
        return propertyRepository.findById(propertyId)
                .orElseThrow(() -> new NotFoundException("Property not found: " + propertyId));
    }

    public RoomType getRoomType(Property property, String roomTypeId) {
        return property.findRoomType(roomTypeId)
                .orElseThrow(() -> new NotFoundException(
                        "Room type " + roomTypeId + " not found in property " + property.id()));
    }

    private Owner getOwner(String ownerId) {
        return ownerRepository.findById(ownerId)
                .orElseThrow(() -> new NotFoundException("Owner not found: " + ownerId));
    }

    private Property toProperty(String ownerId, NewProperty request) {
        List<RoomType> roomTypes = request.roomTypes().stream()
                .map(this::toRoomType)
                .toList();
        CancellationPolicyType policy = request.cancellationPolicy() == null
                ? CancellationPolicyType.FLEXIBLE
                : request.cancellationPolicy();

        return new Property(
                newId(),
                ownerId,
                request.name(),
                request.type(),
                new Location(request.city(), request.locality(), request.addressLine()),
                request.starRating(),
                request.amenities(),
                policy,
                roomTypes);
    }

    private RoomType toRoomType(NewRoomType request) {
        return new RoomType(newId(), request.name(), request.maxGuests(), request.totalRooms(),
                new Money(request.pricePerNight()));
    }

    private static String newId() {
        return UUID.randomUUID().toString();
    }
}

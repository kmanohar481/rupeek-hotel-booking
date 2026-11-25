package com.rupeek.hotel.property.repository;

import com.rupeek.hotel.property.model.Property;

import java.util.List;
import java.util.Optional;

public interface PropertyRepository {

    Property save(Property property);

    Optional<Property> findById(String propertyId);

    List<Property> findByOwnerId(String ownerId);

    List<Property> findAll();
}

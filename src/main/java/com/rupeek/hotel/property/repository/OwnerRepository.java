package com.rupeek.hotel.property.repository;

import com.rupeek.hotel.property.model.Owner;

import java.util.Optional;

public interface OwnerRepository {

    Owner save(Owner owner);

    Optional<Owner> findById(String ownerId);
}

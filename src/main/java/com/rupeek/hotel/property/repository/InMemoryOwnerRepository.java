package com.rupeek.hotel.property.repository;

import com.rupeek.hotel.common.utils.InMemoryRepository;
import com.rupeek.hotel.property.model.Owner;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryOwnerRepository extends InMemoryRepository<Owner> implements OwnerRepository {

    public InMemoryOwnerRepository() {
        super(Owner::id);
    }
}

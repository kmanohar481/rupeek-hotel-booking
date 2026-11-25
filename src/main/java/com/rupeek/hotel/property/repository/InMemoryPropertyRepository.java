package com.rupeek.hotel.property.repository;

import com.rupeek.hotel.common.utils.InMemoryRepository;
import com.rupeek.hotel.property.model.Property;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class InMemoryPropertyRepository extends InMemoryRepository<Property> implements PropertyRepository {

    public InMemoryPropertyRepository() {
        super(Property::id);
    }

    @Override
    public List<Property> findByOwnerId(String ownerId) {
        return stream().filter(property -> property.ownerId().equals(ownerId)).toList();
    }

    @Override
    public List<Property> findAll() {
        return stream().toList();
    }
}

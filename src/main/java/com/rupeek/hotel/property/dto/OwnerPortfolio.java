package com.rupeek.hotel.property.dto;

import com.rupeek.hotel.property.model.Owner;
import com.rupeek.hotel.property.model.Property;

import java.util.List;

public record OwnerPortfolio(Owner owner, List<Property> properties) {
}

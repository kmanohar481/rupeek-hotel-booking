package com.rupeek.hotel.search.service.filter;

import com.rupeek.hotel.property.model.Property;
import com.rupeek.hotel.property.model.RoomType;
import com.rupeek.hotel.search.dto.SearchCriteria;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Compares against the room's base nightly price.
 */
@Component
public class PriceRangeFilter implements SearchFilter {

    @Override
    public boolean matches(Property property, RoomType roomType, SearchCriteria criteria) {
        BigDecimal price = roomType.pricePerNight().amount();
        boolean aboveMin = criteria.minPrice() == null || price.compareTo(criteria.minPrice()) >= 0;
        boolean belowMax = criteria.maxPrice() == null || price.compareTo(criteria.maxPrice()) <= 0;
        return aboveMin && belowMax;
    }
}

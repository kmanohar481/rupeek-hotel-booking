package com.rupeek.hotel.search.service.filter;

import com.rupeek.hotel.property.model.Property;
import com.rupeek.hotel.property.model.RoomType;
import com.rupeek.hotel.search.dto.SearchCriteria;
import org.springframework.stereotype.Component;

@Component
public class LocationFilter implements SearchFilter {

    @Override
    public boolean matches(Property property, RoomType roomType, SearchCriteria criteria) {
        return matchesIfGiven(criteria.city(), property.location().city())
                && matchesIfGiven(criteria.locality(), property.location().locality());
    }

    private boolean matchesIfGiven(String wanted, String actual) {
        return wanted == null || wanted.isBlank() || wanted.trim().equalsIgnoreCase(actual);
    }
}

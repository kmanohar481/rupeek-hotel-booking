package com.rupeek.hotel.search.service.filter;

import com.rupeek.hotel.property.model.Property;
import com.rupeek.hotel.property.model.RoomType;
import com.rupeek.hotel.search.dto.SearchCriteria;
import org.springframework.stereotype.Component;

@Component
public class StarRatingFilter implements SearchFilter {

    @Override
    public boolean matches(Property property, RoomType roomType, SearchCriteria criteria) {
        return criteria.minStarRating() == null || property.starRating() >= criteria.minStarRating();
    }
}

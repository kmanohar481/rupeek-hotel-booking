package com.rupeek.hotel.search.service.filter;

import com.rupeek.hotel.property.model.Property;
import com.rupeek.hotel.property.model.RoomType;
import com.rupeek.hotel.search.dto.SearchCriteria;

/**
 * One search rule. Filters are evaluated per (property, room type) pair so that rules like price and
 * capacity apply to the same room the guest would actually book.
 * Filters for property-level attributes simply ignore the room type.
 * <p>
 * To add a new filter, add a field to {@link SearchCriteria} and a new {@code @Component} implementing this
 * interface - SearchService does not change.
 */
public interface SearchFilter {

    boolean matches(Property property, RoomType roomType, SearchCriteria criteria);
}

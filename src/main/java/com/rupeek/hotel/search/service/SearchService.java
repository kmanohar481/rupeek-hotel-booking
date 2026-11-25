package com.rupeek.hotel.search.service;

import com.rupeek.hotel.inventory.repository.InventoryRepository;
import com.rupeek.hotel.pricing.service.PricingStrategy;
import com.rupeek.hotel.property.model.Property;
import com.rupeek.hotel.property.model.RoomType;
import com.rupeek.hotel.property.repository.PropertyRepository;
import com.rupeek.hotel.search.dto.AvailableRoom;
import com.rupeek.hotel.search.dto.PropertySearchResult;
import com.rupeek.hotel.search.dto.SearchCriteria;
import com.rupeek.hotel.search.service.filter.SearchFilter;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Returns properties that have at least one room type matching every filter and free for the whole stay.
 * <p>
 * Search reads availability without locking, so a result can be stale by the time the guest books;
 * the booking flow re-checks atomically, which is what actually prevents double booking.
 */
@Service
public class SearchService {

    private final PropertyRepository propertyRepository;
    private final InventoryRepository inventoryRepository;
    private final PricingStrategy pricingStrategy;
    private final List<SearchFilter> filters;
    private final Clock clock;

    public SearchService(PropertyRepository propertyRepository,
                         InventoryRepository inventoryRepository,
                         PricingStrategy pricingStrategy,
                         List<SearchFilter> filters,
                         Clock clock) {
        this.propertyRepository = propertyRepository;
        this.inventoryRepository = inventoryRepository;
        this.pricingStrategy = pricingStrategy;
        this.filters = filters;
        this.clock = clock;
    }

    public List<PropertySearchResult> search(SearchCriteria criteria) {
        criteria.stay().requireBookableOn(LocalDate.now(clock));
        return propertyRepository.findAll().stream()
                .map(property -> toResult(property, criteria))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(PropertySearchResult::name))
                .toList();
    }

    private Optional<PropertySearchResult> toResult(Property property, SearchCriteria criteria) {
        List<AvailableRoom> rooms = property.roomTypes().stream()
                .filter(roomType -> matchesAllFilters(property, roomType, criteria))
                .filter(roomType -> isAvailable(roomType, criteria))
                .map(roomType -> toAvailableRoom(roomType, criteria))
                .toList();
        if (rooms.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new PropertySearchResult(property.id(), property.name(), property.type(),
                property.location(), property.starRating(), property.amenities(), rooms));
    }

    private boolean matchesAllFilters(Property property, RoomType roomType, SearchCriteria criteria) {
        return filters.stream().allMatch(filter -> filter.matches(property, roomType, criteria));
    }

    private boolean isAvailable(RoomType roomType, SearchCriteria criteria) {
        return inventoryRepository.isAvailable(roomType.id(), criteria.stay(), roomType.totalRooms());
    }

    private AvailableRoom toAvailableRoom(RoomType roomType, SearchCriteria criteria) {
        return new AvailableRoom(roomType.id(), roomType.name(), roomType.maxGuests(), roomType.pricePerNight(),
                pricingStrategy.priceFor(roomType, criteria.stay()));
    }
}

package com.rupeek.hotel.search.controller;

import com.rupeek.hotel.common.model.DateRange;
import com.rupeek.hotel.property.constants.Amenity;
import com.rupeek.hotel.search.dto.PropertySearchResult;
import com.rupeek.hotel.search.dto.SearchCriteria;
import com.rupeek.hotel.search.service.SearchService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@RestController
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/api/properties/search")
    public List<PropertySearchResult> search(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String locality,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(defaultValue = "1") int guests,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Set<Amenity> amenities,
            @RequestParam(required = false) Integer minRating) {
        SearchCriteria criteria = new SearchCriteria(city, locality, new DateRange(checkIn, checkOut), guests,
                minPrice, maxPrice, amenities, minRating);
        return searchService.search(criteria);
    }
}

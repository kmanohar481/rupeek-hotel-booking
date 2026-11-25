package com.rupeek.hotel.property.controller;

import com.rupeek.hotel.property.model.Property;
import com.rupeek.hotel.property.service.PropertyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/properties")
public class PropertyController {

    private final PropertyService propertyService;

    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @GetMapping("/{propertyId}")
    public Property getProperty(@PathVariable String propertyId) {
        return propertyService.getProperty(propertyId);
    }
}

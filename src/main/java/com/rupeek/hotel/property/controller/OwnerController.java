package com.rupeek.hotel.property.controller;

import com.rupeek.hotel.property.dto.NewOwner;
import com.rupeek.hotel.property.dto.NewProperty;
import com.rupeek.hotel.property.dto.OwnerPortfolio;
import com.rupeek.hotel.property.model.Property;
import com.rupeek.hotel.property.service.PropertyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/owners")
public class OwnerController {

    private final PropertyService propertyService;

    public OwnerController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OwnerPortfolio onboardOwner(@Valid @RequestBody NewOwner request) {
        return propertyService.onboardOwner(request);
    }

    @PostMapping("/{ownerId}/properties")
    @ResponseStatus(HttpStatus.CREATED)
    public Property addProperty(@PathVariable String ownerId, @Valid @RequestBody NewProperty request) {
        return propertyService.addProperty(ownerId, request);
    }

    @GetMapping("/{ownerId}/properties")
    public OwnerPortfolio getPortfolio(@PathVariable String ownerId) {
        return propertyService.getPortfolio(ownerId);
    }
}

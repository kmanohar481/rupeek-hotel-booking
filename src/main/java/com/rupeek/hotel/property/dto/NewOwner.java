package com.rupeek.hotel.property.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Onboards an owner together with their properties: one for a standalone hotel, many for a chain.
 */
public record NewOwner(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotEmpty List<@Valid NewProperty> properties) {
}

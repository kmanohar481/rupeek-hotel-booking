package com.rupeek.hotel.property.model;

/**
 * An account that owns one or more properties. A standalone hotel is simply an owner with one property,
 * a chain is an owner with many - same model, same code path.
 */
public record Owner(String id, String name, String email) {
}

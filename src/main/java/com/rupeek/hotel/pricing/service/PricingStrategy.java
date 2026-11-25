package com.rupeek.hotel.pricing.service;

import com.rupeek.hotel.common.model.DateRange;
import com.rupeek.hotel.common.model.Money;
import com.rupeek.hotel.property.model.RoomType;

/**
 * Decides what a stay costs. Search and booking both use it, so the price shown is the price charged.
 */
public interface PricingStrategy {

    Money priceFor(RoomType roomType, DateRange stay);
}

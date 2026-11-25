package com.rupeek.hotel.pricing.service;

import com.rupeek.hotel.common.model.DateRange;
import com.rupeek.hotel.common.model.Money;
import com.rupeek.hotel.property.model.RoomType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Flat nightly price x number of nights.
 */
@Component
@ConditionalOnProperty(name = "pricing.strategy", havingValue = "standard", matchIfMissing = true)
public class StandardPricingStrategy implements PricingStrategy {

    @Override
    public Money priceFor(RoomType roomType, DateRange stay) {
        Money total = Money.ZERO;
        for (int night = 0; night < stay.numberOfNights(); night++) {
            total = total.plus(roomType.pricePerNight());
        }
        return total;
    }
}

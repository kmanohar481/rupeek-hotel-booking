package com.rupeek.hotel.pricing.service;

import com.rupeek.hotel.common.model.DateRange;
import com.rupeek.hotel.common.model.Money;
import com.rupeek.hotel.property.model.RoomType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

/**
 * Adds a surcharge on Friday and Saturday nights. Enabled with {@code pricing.strategy=weekend}.
 */
@Component
@ConditionalOnProperty(name = "pricing.strategy", havingValue = "weekend")
public class WeekendPricingStrategy implements PricingStrategy {

    private static final Set<DayOfWeek> WEEKEND_NIGHTS = Set.of(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY);

    private final int surchargePercent;

    public WeekendPricingStrategy(@Value("${pricing.weekend-surcharge-percent:20}") int surchargePercent) {
        this.surchargePercent = surchargePercent;
    }

    @Override
    public Money priceFor(RoomType roomType, DateRange stay) {
        return stay.nights().stream()
                .map(night -> nightlyPrice(roomType.pricePerNight(), night))
                .reduce(Money.ZERO, Money::plus);
    }

    private Money nightlyPrice(Money basePrice, LocalDate night) {
        if (WEEKEND_NIGHTS.contains(night.getDayOfWeek())) {
            return basePrice.plus(basePrice.percentage(surchargePercent));
        }
        return basePrice;
    }
}

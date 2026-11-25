package com.rupeek.hotel.pricing.service;

import com.rupeek.hotel.common.model.DateRange;
import com.rupeek.hotel.common.model.Money;
import com.rupeek.hotel.property.model.RoomType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PricingStrategyTest {

    private static final RoomType DELUXE = new RoomType("r1", "Deluxe", 2, 5, Money.of(1000));
    // Thursday 8 Jan -> Sunday 11 Jan 2026: Thursday, Friday and Saturday nights
    private static final DateRange THU_TO_SUN = new DateRange(LocalDate.of(2026, 1, 8), LocalDate.of(2026, 1, 11));

    @Test
    void standardPriceIsNightlyRateTimesNights() {
        assertThat(new StandardPricingStrategy().priceFor(DELUXE, THU_TO_SUN)).isEqualTo(Money.of(3000));
    }

    @Test
    void weekendPricingAddsSurchargeOnFridayAndSaturdayNights() {
        Money price = new WeekendPricingStrategy(20).priceFor(DELUXE, THU_TO_SUN);

        assertThat(price).isEqualTo(Money.of(1000 + 1200 + 1200));
    }
}

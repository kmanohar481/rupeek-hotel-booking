package com.rupeek.hotel.common.model;

import com.rupeek.hotel.common.constants.AppConstants;
import com.rupeek.hotel.common.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DateRangeTest {

    private static final LocalDate JAN_10 = LocalDate.of(2026, 1, 10);

    @Test
    void nightsIncludeCheckInButNotCheckOut() {
        DateRange stay = new DateRange(JAN_10, JAN_10.plusDays(2));

        assertThat(stay.numberOfNights()).isEqualTo(2);
        assertThat(stay.nights()).containsExactly(JAN_10, JAN_10.plusDays(1));
    }

    @Test
    void rejectsCheckOutOnOrBeforeCheckIn() {
        assertThatThrownBy(() -> new DateRange(JAN_10, JAN_10)).isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> new DateRange(JAN_10, JAN_10.minusDays(1))).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void rejectsMissingDates() {
        assertThatThrownBy(() -> new DateRange(null, JAN_10)).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void overlapsOnlyWhenNightsAreShared() {
        DateRange tenToTwelve = new DateRange(JAN_10, JAN_10.plusDays(2));

        assertThat(tenToTwelve.overlaps(new DateRange(JAN_10.plusDays(1), JAN_10.plusDays(3)))).isTrue();
        assertThat(tenToTwelve.overlaps(new DateRange(JAN_10.minusDays(5), JAN_10.plusDays(5)))).isTrue();
        // back-to-back: one guest checks out the morning the next checks in
        assertThat(tenToTwelve.overlaps(new DateRange(JAN_10.plusDays(2), JAN_10.plusDays(4)))).isFalse();
    }

    @Test
    void bookableFromTodayButNotInThePast() {
        new DateRange(JAN_10, JAN_10.plusDays(1)).requireBookableOn(JAN_10);

        assertThatThrownBy(() -> new DateRange(JAN_10, JAN_10.plusDays(1)).requireBookableOn(JAN_10.plusDays(1)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("past");
    }

    @Test
    void acceptsMaximumStayButNothingLonger() {
        new DateRange(JAN_10, JAN_10.plusDays(AppConstants.MAX_STAY_NIGHTS)).requireBookableOn(JAN_10);

        DateRange tooLong = new DateRange(JAN_10, JAN_10.plusDays(AppConstants.MAX_STAY_NIGHTS + 1));
        assertThatThrownBy(() -> tooLong.requireBookableOn(JAN_10)).isInstanceOf(InvalidRequestException.class);
    }
}

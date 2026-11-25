package com.rupeek.hotel.search.service;

import com.rupeek.hotel.booking.dto.NewBooking;
import com.rupeek.hotel.booking.model.Booking;
import com.rupeek.hotel.common.exception.InvalidRequestException;
import com.rupeek.hotel.common.model.DateRange;
import com.rupeek.hotel.common.model.Money;
import com.rupeek.hotel.property.constants.Amenity;
import com.rupeek.hotel.property.model.Property;
import com.rupeek.hotel.search.dto.AvailableRoom;
import com.rupeek.hotel.search.dto.PropertySearchResult;
import com.rupeek.hotel.search.dto.SearchCriteria;
import com.rupeek.hotel.support.TestFixture;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static com.rupeek.hotel.cancellation.constants.CancellationPolicyType.FLEXIBLE;
import static com.rupeek.hotel.support.TestFixture.day;
import static com.rupeek.hotel.support.TestFixture.hotel;
import static com.rupeek.hotel.support.TestFixture.room;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SearchServiceTest {

    private static final DateRange STAY = new DateRange(day(10), day(12));

    private final TestFixture fixture = new TestFixture();
    private final SearchService searchService = fixture.searchService;

    private static SearchCriteria inGoa(int guests) {
        return new SearchCriteria("Goa", null, STAY, guests, null, null, null, null);
    }

    private List<String> names(List<PropertySearchResult> results) {
        return results.stream().map(PropertySearchResult::name).toList();
    }

    @Test
    void findsAvailablePropertiesInTheCityWithStayPrice() {
        fixture.simpleHotel(1);
        fixture.onboard(hotel("Hill Top", "Shimla", "Mall Road", 3, Set.of(), FLEXIBLE, room("Std", 2, 1, 1500)));

        List<PropertySearchResult> results = searchService.search(inGoa(2));

        assertThat(names(results)).containsExactly("Sea Breeze");
        AvailableRoom room = results.get(0).availableRooms().get(0);
        assertThat(room.pricePerNight()).isEqualTo(Money.of(2000));
        assertThat(room.totalPrice()).isEqualTo(Money.of(4000));
    }

    @Test
    void searchesByLocalityAlone() {
        fixture.simpleHotel(1);
        fixture.onboard(hotel("Baga Bay", "Goa", "Baga", 3, Set.of(), FLEXIBLE, room("Std", 2, 1, 1500)));

        List<PropertySearchResult> results = searchService.search(
                new SearchCriteria(null, "baga", STAY, 1, null, null, null, null));

        assertThat(names(results)).containsExactly("Baga Bay");
    }

    @Test
    void soldOutPropertyIsHiddenAndReappearsAfterCancellation() {
        Property hotel = fixture.simpleHotel(1);
        Booking booking = fixture.book(hotel, day(11), day(13));

        assertThat(searchService.search(inGoa(2))).isEmpty();

        fixture.cancellationService.cancel(booking.id());
        assertThat(names(searchService.search(inGoa(2)))).containsExactly("Sea Breeze");
    }

    @Test
    void priceAndAvailabilityAreCheckedOnTheSameRoomType() {
        Property hotel = fixture.onboard(hotel("Mixed", "Goa", "Calangute", 4, Set.of(), FLEXIBLE,
                room("Budget", 2, 1, 1500), room("Suite", 2, 1, 8000)));
        fixture.bookingService.createBooking(new NewBooking(hotel.id(),
                hotel.roomTypes().get(0).id(), "Guest One", STAY.checkIn(), STAY.checkOut(), 2));

        // Budget is in range but sold out, Suite is free but too expensive -> no match
        List<PropertySearchResult> results = searchService.search(new SearchCriteria("Goa", null, STAY, 2,
                new BigDecimal("1000"), new BigDecimal("3000"), null, null));

        assertThat(results).isEmpty();
    }

    @Test
    void returnsOnlyTheRoomTypesThatMatch() {
        fixture.onboard(hotel("Family Stay", "Goa", "Calangute", 3, Set.of(), FLEXIBLE,
                room("Double", 2, 3, 2000), room("Family", 4, 3, 3500)));

        List<PropertySearchResult> results = searchService.search(inGoa(4));

        assertThat(results.get(0).availableRooms()).extracting(AvailableRoom::name).containsExactly("Family");
    }

    @Test
    void filtersByPriceRangeAmenitiesAndRating() {
        fixture.simpleHotel(1); // 4 stars, WIFI + POOL, 2000/night
        fixture.onboard(hotel("Budget Inn", "Goa", "Panjim", 2, Set.of(Amenity.WIFI), FLEXIBLE,
                room("Std", 2, 5, 900)));

        assertThat(names(searchService.search(new SearchCriteria("Goa", null, STAY, 2,
                null, new BigDecimal("1000"), null, null)))).containsExactly("Budget Inn");
        assertThat(names(searchService.search(new SearchCriteria("Goa", null, STAY, 2,
                null, null, Set.of(Amenity.POOL), null)))).containsExactly("Sea Breeze");
        assertThat(names(searchService.search(new SearchCriteria("Goa", null, STAY, 2,
                null, null, null, 3)))).containsExactly("Sea Breeze");
    }

    @Test
    void requiresALocationAndAValidPriceRange() {
        assertThatThrownBy(() -> new SearchCriteria(null, " ", STAY, 1, null, null, null, null))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> new SearchCriteria("Goa", null, STAY, 1,
                new BigDecimal("5000"), new BigDecimal("1000"), null, null))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void rejectsSearchForPastDates() {
        assertThatThrownBy(() -> searchService.search(new SearchCriteria("Goa", null,
                new DateRange(day(-2), day(1)), 1, null, null, null, null)))
                .isInstanceOf(InvalidRequestException.class);
    }
}

package com.rupeek.hotel.property.service;

import com.rupeek.hotel.cancellation.constants.CancellationPolicyType;
import com.rupeek.hotel.common.exception.InvalidRequestException;
import com.rupeek.hotel.common.exception.NotFoundException;
import com.rupeek.hotel.property.constants.Amenity;
import com.rupeek.hotel.property.dto.NewOwner;
import com.rupeek.hotel.property.dto.NewProperty;
import com.rupeek.hotel.property.dto.OwnerPortfolio;
import com.rupeek.hotel.property.model.Property;
import com.rupeek.hotel.support.TestFixture;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static com.rupeek.hotel.support.TestFixture.hotel;
import static com.rupeek.hotel.support.TestFixture.room;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropertyServiceTest {

    private final TestFixture fixture = new TestFixture();
    private final PropertyService propertyService = fixture.propertyService;

    private static NewProperty validHotel(String name) {
        return hotel(name, "Bengaluru", "Indiranagar", 3, Set.of(Amenity.WIFI), null,
                room("Standard", 2, 10, 2500));
    }

    @Test
    void standaloneOwnerIsAnOwnerWithOneProperty() {
        OwnerPortfolio portfolio = propertyService.onboardOwner(
                new NewOwner("Lake View Inn", "inn@example.com", List.of(validHotel("Lake View Inn"))));

        assertThat(portfolio.properties()).hasSize(1);
        assertThat(propertyService.getPortfolio(portfolio.owner().id()).properties()).hasSize(1);
    }

    @Test
    void chainOwnerOnboardsSeveralPropertiesUnderOneAccount() {
        OwnerPortfolio chain = propertyService.onboardOwner(new NewOwner("Comfort Group", "ops@example.com",
                List.of(validHotel("Comfort MG Road"), validHotel("Comfort Koramangala"))));

        Property third = propertyService.addProperty(chain.owner().id(), validHotel("Comfort Whitefield"));

        assertThat(third.ownerId()).isEqualTo(chain.owner().id());
        assertThat(propertyService.getPortfolio(chain.owner().id()).properties())
                .extracting(Property::name)
                .containsExactlyInAnyOrder("Comfort MG Road", "Comfort Koramangala", "Comfort Whitefield");
    }

    @Test
    void nothingIsSavedWhenAnyPropertyInTheRequestIsInvalid() {
        NewProperty invalidRating = hotel("Broken", "Pune", "Baner", 9, Set.of(), null, room("Std", 2, 1, 100));

        assertThatThrownBy(() -> propertyService.onboardOwner(
                new NewOwner("Group", "g@example.com", List.of(validHotel("Fine"), invalidRating))))
                .isInstanceOf(InvalidRequestException.class);
        assertThat(fixture.propertyRepository.findAll()).isEmpty();
    }

    @Test
    void defaultsToFlexibleCancellationPolicy() {
        Property property = fixture.onboard(validHotel("Lake View Inn"));

        assertThat(property.cancellationPolicy()).isEqualTo(CancellationPolicyType.FLEXIBLE);
    }

    @Test
    void rejectsDuplicateRoomTypeNamesAndInvalidRooms() {
        assertThatThrownBy(() -> fixture.onboard(hotel("Dup", "Pune", "Baner", 3, Set.of(), null,
                room("Deluxe", 2, 1, 100), room("deluxe", 3, 1, 200))))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Duplicate");
        assertThatThrownBy(() -> fixture.onboard(hotel("Zero", "Pune", "Baner", 3, Set.of(), null,
                room("Deluxe", 2, 0, 100))))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> fixture.onboard(hotel("Free", "Pune", "Baner", 3, Set.of(), null,
                room("Deluxe", 2, 1, 0))))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void addingPropertyToUnknownOwnerFails() {
        assertThatThrownBy(() -> propertyService.addProperty("missing", validHotel("X")))
                .isInstanceOf(NotFoundException.class);
    }
}

package com.rupeek.hotel.inventory.repository;

import com.rupeek.hotel.common.model.DateRange;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryInventoryRepositoryTest {

    private static final String ROOM = "deluxe";
    private static final LocalDate DAY_1 = LocalDate.of(2026, 1, 10);

    private final InventoryRepository inventory = new InMemoryInventoryRepository();

    private static DateRange nights(int fromDay, int toDay) {
        return new DateRange(DAY_1.plusDays(fromDay - 1), DAY_1.plusDays(toDay - 1));
    }

    @Test
    void reservesUntilAllRoomsAreTaken() {
        assertThat(inventory.tryReserve(ROOM, nights(1, 3), 2)).isTrue();
        assertThat(inventory.tryReserve(ROOM, nights(1, 3), 2)).isTrue();
        assertThat(inventory.tryReserve(ROOM, nights(1, 3), 2)).isFalse();
    }

    @Test
    void overlappingStayIsRejectedWhenAnyNightIsFull() {
        inventory.tryReserve(ROOM, nights(1, 3), 1);

        assertThat(inventory.tryReserve(ROOM, nights(2, 4), 1)).isFalse();
        assertThat(inventory.isAvailable(ROOM, nights(2, 4), 1)).isFalse();
    }

    @Test
    void backToBackStaysShareTheSameRoom() {
        assertThat(inventory.tryReserve(ROOM, nights(1, 3), 1)).isTrue();
        assertThat(inventory.tryReserve(ROOM, nights(3, 5), 1)).isTrue();
    }

    @Test
    void failedReservationLeavesNoPartialHold() {
        inventory.tryReserve(ROOM, nights(2, 3), 1); // night 2 is now full

        assertThat(inventory.tryReserve(ROOM, nights(1, 4), 1)).isFalse();
        assertThat(inventory.bookedRooms(ROOM, DAY_1)).isZero();
        assertThat(inventory.bookedRooms(ROOM, DAY_1.plusDays(2))).isZero();
    }

    @Test
    void releasedRoomCanBeBookedAgain() {
        inventory.tryReserve(ROOM, nights(1, 3), 1);
        inventory.release(ROOM, nights(1, 3));

        assertThat(inventory.isAvailable(ROOM, nights(1, 3), 1)).isTrue();
        assertThat(inventory.tryReserve(ROOM, nights(1, 3), 1)).isTrue();
    }

    @Test
    void releasingARoomThatWasNeverReservedFails() {
        assertThatThrownBy(() -> inventory.release(ROOM, nights(1, 2))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void roomTypesAreTrackedIndependently() {
        inventory.tryReserve(ROOM, nights(1, 3), 1);

        assertThat(inventory.tryReserve("suite", nights(1, 3), 1)).isTrue();
    }
}

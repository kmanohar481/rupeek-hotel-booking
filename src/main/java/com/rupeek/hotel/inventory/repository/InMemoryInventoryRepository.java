package com.rupeek.hotel.inventory.repository;

import com.rupeek.hotel.common.model.DateRange;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryInventoryRepository implements InventoryRepository {

    private final Map<String, RoomCalendar> calendars = new ConcurrentHashMap<>();

    @Override
    public boolean isAvailable(String roomTypeId, DateRange stay, int totalRooms) {
        return calendarFor(roomTypeId).hasRoomEveryNight(stay, totalRooms);
    }

    @Override
    public boolean tryReserve(String roomTypeId, DateRange stay, int totalRooms) {
        return calendarFor(roomTypeId).tryReserve(stay, totalRooms);
    }

    @Override
    public void release(String roomTypeId, DateRange stay) {
        calendarFor(roomTypeId).release(stay);
    }

    @Override
    public int bookedRooms(String roomTypeId, LocalDate night) {
        return calendarFor(roomTypeId).bookedOn(night);
    }

    private RoomCalendar calendarFor(String roomTypeId) {
        return calendars.computeIfAbsent(roomTypeId, id -> new RoomCalendar());
    }

    /**
     * Booked-room count per night for one room type. All methods are synchronized on the calendar,
     * so bookings for the same room type are serialized while different room types proceed in parallel.
     */
    private static final class RoomCalendar {

        private final Map<LocalDate, Integer> bookedPerNight = new HashMap<>();

        synchronized boolean hasRoomEveryNight(DateRange stay, int totalRooms) {
            return stay.nights().stream().allMatch(night -> bookedOn(night) < totalRooms);
        }

        synchronized boolean tryReserve(DateRange stay, int totalRooms) {
            if (!hasRoomEveryNight(stay, totalRooms)) {
                return false;
            }
            stay.nights().forEach(night -> bookedPerNight.merge(night, 1, Integer::sum));
            return true;
        }

        synchronized void release(DateRange stay) {
            for (LocalDate night : stay.nights()) {
                if (bookedOn(night) == 0) {
                    throw new IllegalStateException("Releasing a room that was never reserved on " + night);
                }
            }
            stay.nights().forEach(night -> bookedPerNight.merge(night, -1, Integer::sum));
        }

        synchronized int bookedOn(LocalDate night) {
            return bookedPerNight.getOrDefault(night, 0);
        }
    }
}

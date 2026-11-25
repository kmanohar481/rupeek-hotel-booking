package com.rupeek.hotel.inventory.repository;

import com.rupeek.hotel.common.model.DateRange;

import java.time.LocalDate;

/**
 * Tracks how many rooms of a room type are taken on each night.
 * <p>
 * {@link #tryReserve} must be atomic: either every night of the stay is reserved or none is.
 * The in-memory implementation uses a lock per room type; a database implementation would use a
 * conditional update ({@code UPDATE ... SET booked = booked + 1 WHERE booked < total}).
 */
public interface InventoryRepository {

    boolean isAvailable(String roomTypeId, DateRange stay, int totalRooms);

    boolean tryReserve(String roomTypeId, DateRange stay, int totalRooms);

    void release(String roomTypeId, DateRange stay);

    int bookedRooms(String roomTypeId, LocalDate night);
}

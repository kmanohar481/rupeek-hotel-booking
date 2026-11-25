package com.rupeek.hotel.booking.service;

import com.rupeek.hotel.booking.constants.BookingStatus;
import com.rupeek.hotel.booking.dto.NewBooking;
import com.rupeek.hotel.booking.model.Booking;
import com.rupeek.hotel.booking.repository.BookingRepository;
import com.rupeek.hotel.common.exception.InvalidRequestException;
import com.rupeek.hotel.common.exception.NotFoundException;
import com.rupeek.hotel.common.exception.RoomNotAvailableException;
import com.rupeek.hotel.common.model.DateRange;
import com.rupeek.hotel.common.model.Money;
import com.rupeek.hotel.common.utils.KeyedLock;
import com.rupeek.hotel.inventory.repository.InventoryRepository;
import com.rupeek.hotel.pricing.service.PricingStrategy;
import com.rupeek.hotel.property.model.Property;
import com.rupeek.hotel.property.model.RoomType;
import com.rupeek.hotel.property.service.PropertyService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Function;

@Service
public class BookingService {

    private final PropertyService propertyService;
    private final InventoryRepository inventoryRepository;
    private final PricingStrategy pricingStrategy;
    private final BookingRepository bookingRepository;
    private final Clock clock;
    private final Duration holdDuration;
    private final KeyedLock bookingLocks = new KeyedLock();

    public BookingService(PropertyService propertyService,
                          InventoryRepository inventoryRepository,
                          PricingStrategy pricingStrategy,
                          BookingRepository bookingRepository,
                          Clock clock,
                          @Value("${booking.hold-duration:PT15M}") Duration holdDuration) {
        this.propertyService = propertyService;
        this.inventoryRepository = inventoryRepository;
        this.pricingStrategy = pricingStrategy;
        this.bookingRepository = bookingRepository;
        this.clock = clock;
        this.holdDuration = holdDuration;
    }

    /**
     * Holds one room for the stay and creates a booking awaiting payment.
     * The inventory check-and-reserve is atomic, so two guests can never get the last room.
     */
    public Booking createBooking(NewBooking request) {
        Property property = propertyService.getProperty(request.propertyId());
        RoomType roomType = propertyService.getRoomType(property, request.roomTypeId());
        DateRange stay = new DateRange(request.checkIn(), request.checkOut());
        stay.requireBookableOn(LocalDate.now(clock));
        if (!roomType.canAccommodate(request.guests())) {
            throw new InvalidRequestException(
                    roomType.name() + " allows at most " + roomType.maxGuests() + " guests");
        }

        Money price = pricingStrategy.priceFor(roomType, stay);
        if (!inventoryRepository.tryReserve(roomType.id(), stay, roomType.totalRooms())) {
            throw new RoomNotAvailableException(roomType.name() + " is not available for " + stay);
        }

        Booking booking = new Booking(UUID.randomUUID().toString(), property.id(), roomType.id(),
                request.guestName(), stay, request.guests(), price, clock.instant());
        try {
            return bookingRepository.save(booking);
        } catch (RuntimeException e) {
            inventoryRepository.release(roomType.id(), stay);
            throw e;
        }
    }

    public Booking getBooking(String bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));
    }

    /**
     * Runs {@code action} while holding this booking's lock. Every status change (pay, cancel, expire)
     * goes through here, so they can never interleave for the same booking.
     * Lock order is always booking -> room type (inventory), which rules out deadlocks.
     */
    public <T> T withLockedBooking(String bookingId, Function<Booking, T> action) {
        return bookingLocks.withLock(bookingId, () -> {
            Booking booking = getBooking(bookingId);
            T result = action.apply(booking);
            bookingRepository.save(booking);
            return result;
        });
    }

    public void releaseInventory(Booking booking) {
        inventoryRepository.release(booking.roomTypeId(), booking.stay());
    }

    /**
     * Expires bookings that were never paid within the hold window and frees their rooms.
     *
     * @return number of bookings expired
     */
    public int expireUnpaidBookings() {
        int expired = 0;
        for (Booking candidate : bookingRepository.findByStatus(BookingStatus.PENDING_PAYMENT)) {
            boolean didExpire = withLockedBooking(candidate.id(), booking -> {
                // re-check under the lock: the guest may have paid or cancelled meanwhile
                if (!booking.isHoldExpired(clock.instant(), holdDuration)) {
                    return false;
                }
                booking.expire();
                releaseInventory(booking);
                return true;
            });
            if (didExpire) {
                expired++;
            }
        }
        return expired;
    }
}

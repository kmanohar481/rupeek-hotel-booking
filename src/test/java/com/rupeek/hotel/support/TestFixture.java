package com.rupeek.hotel.support;

import com.rupeek.hotel.booking.dto.NewBooking;
import com.rupeek.hotel.booking.model.Booking;
import com.rupeek.hotel.booking.repository.InMemoryBookingRepository;
import com.rupeek.hotel.booking.service.BookingService;
import com.rupeek.hotel.cancellation.constants.CancellationPolicyType;
import com.rupeek.hotel.cancellation.service.CancellationService;
import com.rupeek.hotel.cancellation.service.policy.FlexibleCancellationPolicy;
import com.rupeek.hotel.cancellation.service.policy.NonRefundableCancellationPolicy;
import com.rupeek.hotel.inventory.repository.InMemoryInventoryRepository;
import com.rupeek.hotel.inventory.repository.InventoryRepository;
import com.rupeek.hotel.payment.constants.PaymentMethod;
import com.rupeek.hotel.payment.dto.PaymentRequest;
import com.rupeek.hotel.payment.dto.PaymentResult;
import com.rupeek.hotel.payment.repository.InMemoryPaymentRepository;
import com.rupeek.hotel.payment.service.PaymentService;
import com.rupeek.hotel.payment.service.processor.CardPaymentProcessor;
import com.rupeek.hotel.payment.service.processor.UpiPaymentProcessor;
import com.rupeek.hotel.payment.service.processor.WalletPaymentProcessor;
import com.rupeek.hotel.pricing.service.StandardPricingStrategy;
import com.rupeek.hotel.property.constants.Amenity;
import com.rupeek.hotel.property.constants.PropertyType;
import com.rupeek.hotel.property.dto.NewOwner;
import com.rupeek.hotel.property.dto.NewProperty;
import com.rupeek.hotel.property.dto.NewRoomType;
import com.rupeek.hotel.property.model.Property;
import com.rupeek.hotel.property.repository.InMemoryOwnerRepository;
import com.rupeek.hotel.property.repository.InMemoryPropertyRepository;
import com.rupeek.hotel.property.repository.PropertyRepository;
import com.rupeek.hotel.property.service.PropertyService;
import com.rupeek.hotel.search.service.SearchService;
import com.rupeek.hotel.search.service.filter.AmenitiesFilter;
import com.rupeek.hotel.search.service.filter.GuestCapacityFilter;
import com.rupeek.hotel.search.service.filter.LocationFilter;
import com.rupeek.hotel.search.service.filter.PriceRangeFilter;
import com.rupeek.hotel.search.service.filter.StarRatingFilter;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Wires the real services with in-memory repositories and a controllable clock - no Spring needed.
 * "Today" is Monday 5 Jan 2026.
 */
public class TestFixture {

    public static final LocalDate TODAY = LocalDate.of(2026, 1, 5);
    public static final Duration HOLD_DURATION = Duration.ofMinutes(15);
    public static final String VALID_CARD = "4111111111111111";
    public static final String DECLINED_CARD = "4000000000000002";

    public final MutableClock clock = new MutableClock(TODAY.atTime(10, 0).toInstant(ZoneOffset.UTC));
    public final PropertyRepository propertyRepository = new InMemoryPropertyRepository();
    public final InventoryRepository inventory = new InMemoryInventoryRepository();
    public final InMemoryPaymentRepository paymentRepository = new InMemoryPaymentRepository();
    public final CountingPaymentGateway gateway = new CountingPaymentGateway();

    public final PropertyService propertyService =
            new PropertyService(new InMemoryOwnerRepository(), propertyRepository);
    public final BookingService bookingService = new BookingService(propertyService, inventory,
            new StandardPricingStrategy(), new InMemoryBookingRepository(), clock, HOLD_DURATION);
    public final PaymentService paymentService = new PaymentService(bookingService, paymentRepository, gateway,
            List.of(new CardPaymentProcessor(gateway), new UpiPaymentProcessor(gateway),
                    new WalletPaymentProcessor(gateway)));
    public final CancellationService cancellationService = new CancellationService(bookingService,
            propertyService, paymentService, clock,
            List.of(new FlexibleCancellationPolicy(), new NonRefundableCancellationPolicy()));
    public final SearchService searchService = new SearchService(propertyRepository, inventory,
            new StandardPricingStrategy(),
            List.of(new LocationFilter(), new GuestCapacityFilter(), new PriceRangeFilter(),
                    new AmenitiesFilter(), new StarRatingFilter()),
            clock);

    public static LocalDate day(int daysFromToday) {
        return TODAY.plusDays(daysFromToday);
    }

    public static NewRoomType room(String name, int maxGuests, int totalRooms, long pricePerNight) {
        return new NewRoomType(name, maxGuests, totalRooms, BigDecimal.valueOf(pricePerNight));
    }

    public static NewProperty hotel(String name, String city, String locality, int stars, Set<Amenity> amenities,
                                    CancellationPolicyType policy, NewRoomType... rooms) {
        return new NewProperty(name, PropertyType.HOTEL, city, locality, "1 Beach Road", stars, amenities, policy,
                List.of(rooms));
    }

    public Property onboard(NewProperty property) {
        return propertyService.onboardOwner(new NewOwner("Owner", "owner@example.com", List.of(property)))
                .properties().get(0);
    }

    /**
     * Goa / Calangute, 4 stars, WIFI + POOL, flexible policy, one "Deluxe" room type for 2 guests at 2000/night.
     */
    public Property simpleHotel(int totalRooms) {
        return onboard(hotel("Sea Breeze", "Goa", "Calangute", 4, Set.of(Amenity.WIFI, Amenity.POOL),
                CancellationPolicyType.FLEXIBLE, room("Deluxe", 2, totalRooms, 2000)));
    }

    public Booking book(Property property, LocalDate checkIn, LocalDate checkOut) {
        return bookingService.createBooking(new NewBooking(property.id(), property.roomTypes().get(0).id(),
                "Guest One", checkIn, checkOut, 2));
    }

    public PaymentResult payByCard(String bookingId, String cardNumber) {
        return paymentService.pay(bookingId, UUID.randomUUID().toString(),
                new PaymentRequest(PaymentMethod.CARD, Map.of("cardNumber", cardNumber)));
    }

    public Booking confirmedBooking(Property property, LocalDate checkIn, LocalDate checkOut) {
        Booking booking = book(property, checkIn, checkOut);
        payByCard(booking.id(), VALID_CARD);
        return booking;
    }

    public int bookedRooms(Property property, LocalDate night) {
        return inventory.bookedRooms(property.roomTypes().get(0).id(), night);
    }
}

package com.rupeek.hotel;

import com.rupeek.hotel.booking.constants.BookingStatus;
import com.rupeek.hotel.booking.model.Booking;
import com.rupeek.hotel.common.exception.InvalidStateTransitionException;
import com.rupeek.hotel.common.exception.RoomNotAvailableException;
import com.rupeek.hotel.payment.constants.PaymentMethod;
import com.rupeek.hotel.payment.dto.PaymentRequest;
import com.rupeek.hotel.payment.dto.PaymentResult;
import com.rupeek.hotel.property.model.Property;
import com.rupeek.hotel.support.TestFixture;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static com.rupeek.hotel.support.TestFixture.VALID_CARD;
import static com.rupeek.hotel.support.TestFixture.day;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Races requests against shared state. Every task waits on a latch so they all start together.
 */
class ConcurrencyTest {

    private final TestFixture fixture = new TestFixture();
    private final Property hotel = fixture.simpleHotel(1);

    @Test
    void twentyGuestsRaceForTheLastRoomAndExactlyOneWins() throws Exception {
        List<Outcome> outcomes = race(IntStream.range(0, 20)
                .<Callable<Object>>mapToObj(i -> () -> fixture.book(hotel, day(10), day(12)))
                .toList());

        assertThat(successes(outcomes)).isEqualTo(1);
        assertThat(failuresOf(outcomes, RoomNotAvailableException.class)).isEqualTo(19);
        assertThat(fixture.bookedRooms(hotel, day(10))).isEqualTo(1);
        assertThat(fixture.bookedRooms(hotel, day(11))).isEqualTo(1);
    }

    @Test
    void partiallyOverlappingStaysNeverOversellTheSharedNight() throws Exception {
        // 10..12 and 11..13 share night 11
        List<Outcome> outcomes = race(IntStream.range(0, 20)
                .<Callable<Object>>mapToObj(i -> i % 2 == 0
                        ? () -> fixture.book(hotel, day(10), day(12))
                        : () -> fixture.book(hotel, day(11), day(13)))
                .toList());

        assertThat(successes(outcomes)).isEqualTo(1);
        assertThat(fixture.bookedRooms(hotel, day(11))).isEqualTo(1);
    }

    @Test
    void nonOverlappingStaysCanAllBeBookedAtTheSameTime() throws Exception {
        List<Outcome> outcomes = race(IntStream.range(0, 10)
                .<Callable<Object>>mapToObj(i -> () -> fixture.book(hotel, day(10 + i), day(11 + i)))
                .toList());

        assertThat(successes(outcomes)).isEqualTo(10);
    }

    @Test
    void concurrentRetriesWithSameIdempotencyKeyChargeOnlyOnce() throws Exception {
        Booking booking = fixture.book(hotel, day(10), day(12));
        PaymentRequest request = new PaymentRequest(PaymentMethod.CARD, Map.of("cardNumber", VALID_CARD));

        List<Outcome> outcomes = race(IntStream.range(0, 10)
                .<Callable<Object>>mapToObj(i -> () -> fixture.paymentService.pay(booking.id(), "same-key", request))
                .toList());

        assertThat(successes(outcomes)).isEqualTo(10);
        assertThat(outcomes).extracting(o -> ((PaymentResult) o.value()).payment().id()).containsOnly(
                ((PaymentResult) outcomes.get(0).value()).payment().id());
        assertThat(fixture.gateway.charges()).isEqualTo(1);
    }

    @Test
    void concurrentPaymentsWithDifferentKeysChargeOnlyOnce() throws Exception {
        Booking booking = fixture.book(hotel, day(10), day(12));

        List<Outcome> outcomes = race(IntStream.range(0, 10)
                .<Callable<Object>>mapToObj(i -> () -> fixture.payByCard(booking.id(), VALID_CARD))
                .toList());

        assertThat(successes(outcomes)).isEqualTo(1);
        assertThat(failuresOf(outcomes, InvalidStateTransitionException.class)).isEqualTo(9);
        assertThat(fixture.gateway.charges()).isEqualTo(1);
    }

    @Test
    void payAndCancelRacingAlwaysLeaveBookingAndInventoryConsistent() throws Exception {
        for (int i = 0; i < 50; i++) {
            Booking booking = fixture.book(hotel, day(10 + i), day(11 + i));

            race(List.of(
                    () -> fixture.payByCard(booking.id(), VALID_CARD),
                    () -> fixture.cancellationService.cancel(booking.id())));

            int roomsHeld = fixture.bookedRooms(hotel, day(10 + i));
            if (booking.status() == BookingStatus.CONFIRMED) {
                assertThat(roomsHeld).as("confirmed booking must keep its room").isEqualTo(1);
            } else {
                assertThat(booking.status()).isEqualTo(BookingStatus.CANCELLED);
                assertThat(roomsHeld).as("cancelled booking must free its room").isZero();
            }
        }
    }

    @Test
    void concurrentCancellationsRefundAndReleaseOnlyOnce() throws Exception {
        Booking booking = fixture.confirmedBooking(hotel, day(10), day(12));

        List<Outcome> outcomes = race(IntStream.range(0, 10)
                .<Callable<Object>>mapToObj(i -> () -> fixture.cancellationService.cancel(booking.id()))
                .toList());

        assertThat(successes(outcomes)).isEqualTo(1);
        assertThat(fixture.gateway.refunds()).isEqualTo(1);
        assertThat(fixture.bookedRooms(hotel, day(10))).isZero();
    }

    // --- helpers ---

    private record Outcome(Object value, Throwable error) {
    }

    private static List<Outcome> race(List<Callable<Object>> tasks) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch startTogether = new CountDownLatch(1);
        List<Future<Object>> futures = new ArrayList<>();
        for (Callable<Object> task : tasks) {
            futures.add(pool.submit(() -> {
                startTogether.await();
                return task.call();
            }));
        }
        startTogether.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        List<Outcome> outcomes = new ArrayList<>();
        for (Future<Object> future : futures) {
            try {
                outcomes.add(new Outcome(future.get(), null));
            } catch (ExecutionException e) {
                outcomes.add(new Outcome(null, e.getCause()));
            }
        }
        return outcomes;
    }

    private static long successes(List<Outcome> outcomes) {
        return outcomes.stream().filter(outcome -> outcome.error() == null).count();
    }

    private static long failuresOf(List<Outcome> outcomes, Class<? extends Throwable> type) {
        return outcomes.stream().filter(outcome -> type.isInstance(outcome.error())).count();
    }
}

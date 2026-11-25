package com.rupeek.hotel.payment.service;

import com.rupeek.hotel.booking.constants.BookingStatus;
import com.rupeek.hotel.booking.model.Booking;
import com.rupeek.hotel.common.exception.InvalidRequestException;
import com.rupeek.hotel.common.exception.InvalidStateTransitionException;
import com.rupeek.hotel.payment.constants.PaymentMethod;
import com.rupeek.hotel.payment.constants.PaymentStatus;
import com.rupeek.hotel.payment.dto.PaymentRequest;
import com.rupeek.hotel.payment.dto.PaymentResult;
import com.rupeek.hotel.property.model.Property;
import com.rupeek.hotel.support.TestFixture;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.rupeek.hotel.support.TestFixture.DECLINED_CARD;
import static com.rupeek.hotel.support.TestFixture.VALID_CARD;
import static com.rupeek.hotel.support.TestFixture.day;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentServiceTest {

    private final TestFixture fixture = new TestFixture();
    private final PaymentService paymentService = fixture.paymentService;
    private final Property hotel = fixture.simpleHotel(1);
    private final Booking booking = fixture.book(hotel, day(10), day(12));

    private static PaymentRequest card(String number) {
        return new PaymentRequest(PaymentMethod.CARD, Map.of("cardNumber", number));
    }

    @Test
    void successfulPaymentConfirmsBookingAndChargesTheBookingAmount() {
        PaymentResult result = paymentService.pay(booking.id(), "key-1", card(VALID_CARD));

        assertThat(result.payment().status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(result.payment().amount()).isEqualTo(booking.totalAmount());
        assertThat(result.bookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(fixture.bookedRooms(hotel, day(10))).isEqualTo(1);
    }

    @Test
    void declinedPaymentFailsBookingAndReleasesTheRoom() {
        PaymentResult result = paymentService.pay(booking.id(), "key-1", card(DECLINED_CARD));

        assertThat(result.payment().status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(booking.status()).isEqualTo(BookingStatus.PAYMENT_FAILED);
        assertThat(fixture.bookedRooms(hotel, day(10))).isZero();
    }

    @Test
    void retryWithSameIdempotencyKeyReturnsOriginalResultWithoutChargingAgain() {
        PaymentResult first = paymentService.pay(booking.id(), "key-1", card(VALID_CARD));
        PaymentResult retry = paymentService.pay(booking.id(), "key-1", card(VALID_CARD));

        assertThat(retry.payment().id()).isEqualTo(first.payment().id());
        assertThat(retry.bookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(fixture.gateway.charges()).isEqualTo(1);
    }

    @Test
    void secondPaymentWithNewKeyIsRejectedBeforeCharging() {
        paymentService.pay(booking.id(), "key-1", card(VALID_CARD));

        assertThatThrownBy(() -> paymentService.pay(booking.id(), "key-2", card(VALID_CARD)))
                .isInstanceOf(InvalidStateTransitionException.class);
        assertThat(fixture.gateway.charges()).isEqualTo(1);
    }

    @Test
    void cancelledBookingCannotBePaid() {
        fixture.cancellationService.cancel(booking.id());

        assertThatThrownBy(() -> paymentService.pay(booking.id(), "key-1", card(VALID_CARD)))
                .isInstanceOf(InvalidStateTransitionException.class);
        assertThat(fixture.gateway.charges()).isZero();
    }

    @Test
    void supportsUpiAndWallet() {
        Booking second = fixture.book(fixture.simpleHotel(1), day(10), day(11));

        PaymentResult upi = paymentService.pay(booking.id(), "k1",
                new PaymentRequest(PaymentMethod.UPI, Map.of("vpa", "guest@okbank")));
        PaymentResult wallet = paymentService.pay(second.id(), "k2",
                new PaymentRequest(PaymentMethod.WALLET, Map.of("walletId", "wallet-123")));

        assertThat(upi.bookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(wallet.bookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void invalidPaymentDetailsAreRejectedAndBookingStaysPayable() {
        assertThatThrownBy(() -> paymentService.pay(booking.id(), "k1",
                new PaymentRequest(PaymentMethod.UPI, Map.of("vpa", "not-a-vpa"))))
                .isInstanceOf(InvalidRequestException.class);

        assertThat(booking.status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(paymentService.pay(booking.id(), "k2", card(VALID_CARD)).bookingStatus())
                .isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void idempotencyKeyIsRequired() {
        assertThatThrownBy(() -> paymentService.pay(booking.id(), " ", card(VALID_CARD)))
                .isInstanceOf(InvalidRequestException.class);
    }
}

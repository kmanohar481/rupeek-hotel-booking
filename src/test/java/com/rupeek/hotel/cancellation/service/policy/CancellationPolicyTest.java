package com.rupeek.hotel.cancellation.service.policy;

import com.rupeek.hotel.common.model.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class CancellationPolicyTest {

    private static final Money PAID = Money.of(10000);

    @ParameterizedTest(name = "{0} days before check-in refunds {1}")
    @CsvSource({
            "30, 10000",
            "7, 10000",
            "6, 5000",
            "2, 5000",
            "1, 0",
            "0, 0"
    })
    void flexiblePolicyRefundsByNoticeGiven(long daysBeforeCheckIn, long expectedRefund) {
        Money refund = new FlexibleCancellationPolicy().refundFor(PAID, daysBeforeCheckIn);

        assertThat(refund).isEqualTo(Money.of(expectedRefund));
    }

    @Test
    void nonRefundablePolicyNeverRefunds() {
        assertThat(new NonRefundableCancellationPolicy().refundFor(PAID, 60)).isEqualTo(Money.ZERO);
    }
}

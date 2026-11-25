package com.rupeek.hotel.cancellation.service.policy;

import com.rupeek.hotel.cancellation.constants.CancellationPolicyType;
import com.rupeek.hotel.common.model.Money;
import org.springframework.stereotype.Component;

/**
 * Full refund a week or more before check-in, half refund 2-6 days before, nothing after that.
 */
@Component
public class FlexibleCancellationPolicy implements CancellationPolicy {

    @Override
    public CancellationPolicyType type() {
        return CancellationPolicyType.FLEXIBLE;
    }

    @Override
    public Money refundFor(Money amountPaid, long daysBeforeCheckIn) {
        if (daysBeforeCheckIn >= 7) {
            return amountPaid;
        }
        if (daysBeforeCheckIn >= 2) {
            return amountPaid.percentage(50);
        }
        return Money.ZERO;
    }
}

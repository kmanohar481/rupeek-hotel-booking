package com.rupeek.hotel.cancellation.service.policy;

import com.rupeek.hotel.cancellation.constants.CancellationPolicyType;
import com.rupeek.hotel.common.model.Money;
import org.springframework.stereotype.Component;

@Component
public class NonRefundableCancellationPolicy implements CancellationPolicy {

    @Override
    public CancellationPolicyType type() {
        return CancellationPolicyType.NON_REFUNDABLE;
    }

    @Override
    public Money refundFor(Money amountPaid, long daysBeforeCheckIn) {
        return Money.ZERO;
    }
}

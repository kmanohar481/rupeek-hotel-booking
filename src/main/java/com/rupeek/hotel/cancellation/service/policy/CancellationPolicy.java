package com.rupeek.hotel.cancellation.service.policy;

import com.rupeek.hotel.cancellation.constants.CancellationPolicyType;
import com.rupeek.hotel.common.model.Money;

/**
 * Decides how much of a paid amount is refunded when a guest cancels.
 * A new policy = a new enum value + a new bean; CancellationService picks it up automatically.
 */
public interface CancellationPolicy {

    CancellationPolicyType type();

    Money refundFor(Money amountPaid, long daysBeforeCheckIn);
}

package com.rupeek.hotel.cancellation.constants;

import com.rupeek.hotel.cancellation.service.policy.CancellationPolicy;

/**
 * Cancellation terms an owner picks for a property. Each value has a matching {@link CancellationPolicy} bean.
 */
public enum CancellationPolicyType {
    FLEXIBLE,
    NON_REFUNDABLE
}

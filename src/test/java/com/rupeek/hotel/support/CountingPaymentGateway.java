package com.rupeek.hotel.support;

import com.rupeek.hotel.common.model.Money;
import com.rupeek.hotel.payment.gateway.ChargeRequest;
import com.rupeek.hotel.payment.gateway.GatewayResponse;
import com.rupeek.hotel.payment.gateway.MockPaymentGateway;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Mock gateway that remembers how many times it was called, to prove we never double charge.
 */
public class CountingPaymentGateway extends MockPaymentGateway {

    private final AtomicInteger charges = new AtomicInteger();
    private final AtomicInteger refunds = new AtomicInteger();

    @Override
    public GatewayResponse charge(ChargeRequest request) {
        charges.incrementAndGet();
        return super.charge(request);
    }

    @Override
    public GatewayResponse refund(String chargeReference, Money amount) {
        refunds.incrementAndGet();
        return super.refund(chargeReference, amount);
    }

    public int charges() {
        return charges.get();
    }

    public int refunds() {
        return refunds.get();
    }
}

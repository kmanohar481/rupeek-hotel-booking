package com.rupeek.hotel.payment.gateway;

import com.rupeek.hotel.common.model.Money;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

/**
 * Stand-in for a real gateway. Every charge succeeds except for these test instruments,
 * which lets the failure path be exercised from the API.
 */
@Component
public class MockPaymentGateway implements PaymentGateway {

    public static final Set<String> DECLINED_INSTRUMENTS = Set.of("4000000000000002", "fail@upi", "fail-wallet");

    @Override
    public GatewayResponse charge(ChargeRequest request) {
        if (DECLINED_INSTRUMENTS.contains(request.instrument())) {
            return new GatewayResponse(false, null, "Declined by issuer");
        }
        return new GatewayResponse(true, "chg_" + UUID.randomUUID(), "Approved");
    }

    @Override
    public GatewayResponse refund(String chargeReference, Money amount) {
        return new GatewayResponse(true, "rfd_" + UUID.randomUUID(), "Refunded " + amount);
    }
}

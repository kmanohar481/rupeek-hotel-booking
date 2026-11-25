package com.rupeek.hotel.payment.gateway;

import com.rupeek.hotel.common.model.Money;

/**
 * Our boundary to the external payment provider.
 */
public interface PaymentGateway {

    GatewayResponse charge(ChargeRequest request);

    GatewayResponse refund(String chargeReference, Money amount);
}

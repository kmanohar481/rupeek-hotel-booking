package com.rupeek.hotel.payment.service.processor;

import com.rupeek.hotel.common.model.Money;
import com.rupeek.hotel.payment.constants.PaymentMethod;
import com.rupeek.hotel.payment.gateway.GatewayResponse;

import java.util.Map;

/**
 * One implementation per payment method. A new method = a new enum value + a new processor bean;
 * PaymentService picks it up automatically.
 */
public interface PaymentProcessor {

    PaymentMethod method();

    /**
     * Validates the method-specific details and charges the amount.
     */
    GatewayResponse charge(Money amount, Map<String, String> details);
}

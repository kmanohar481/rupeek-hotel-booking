package com.rupeek.hotel.payment.gateway;

import com.rupeek.hotel.common.model.Money;
import com.rupeek.hotel.payment.constants.PaymentMethod;

/**
 * @param instrument card number, UPI id or wallet id - whatever the gateway needs to debit
 */
public record ChargeRequest(PaymentMethod method, String instrument, Money amount) {
}

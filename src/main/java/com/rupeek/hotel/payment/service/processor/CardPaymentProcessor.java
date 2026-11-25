package com.rupeek.hotel.payment.service.processor;

import com.rupeek.hotel.common.model.Money;
import com.rupeek.hotel.payment.constants.PaymentMethod;
import com.rupeek.hotel.payment.gateway.ChargeRequest;
import com.rupeek.hotel.payment.gateway.GatewayResponse;
import com.rupeek.hotel.payment.gateway.PaymentGateway;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;

@Component
public class CardPaymentProcessor implements PaymentProcessor {

    private static final Pattern CARD_NUMBER_FORMAT = Pattern.compile("\\d{12,19}");

    private final PaymentGateway gateway;

    public CardPaymentProcessor(PaymentGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public PaymentMethod method() {
        return PaymentMethod.CARD;
    }

    @Override
    public GatewayResponse charge(Money amount, Map<String, String> details) {
        String cardNumber = PaymentDetails.require(details, "cardNumber", CARD_NUMBER_FORMAT);
        return gateway.charge(new ChargeRequest(method(), cardNumber, amount));
    }
}

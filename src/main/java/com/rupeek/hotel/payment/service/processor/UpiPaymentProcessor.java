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
public class UpiPaymentProcessor implements PaymentProcessor {

    private static final Pattern VPA_FORMAT = Pattern.compile("[\\w.-]+@[\\w]+");

    private final PaymentGateway gateway;

    public UpiPaymentProcessor(PaymentGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public PaymentMethod method() {
        return PaymentMethod.UPI;
    }

    @Override
    public GatewayResponse charge(Money amount, Map<String, String> details) {
        String vpa = PaymentDetails.require(details, "vpa", VPA_FORMAT);
        return gateway.charge(new ChargeRequest(method(), vpa, amount));
    }
}

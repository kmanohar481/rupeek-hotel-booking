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
public class WalletPaymentProcessor implements PaymentProcessor {

    private static final Pattern WALLET_ID_FORMAT = Pattern.compile("[\\w-]{3,40}");

    private final PaymentGateway gateway;

    public WalletPaymentProcessor(PaymentGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public PaymentMethod method() {
        return PaymentMethod.WALLET;
    }

    @Override
    public GatewayResponse charge(Money amount, Map<String, String> details) {
        String walletId = PaymentDetails.require(details, "walletId", WALLET_ID_FORMAT);
        return gateway.charge(new ChargeRequest(method(), walletId, amount));
    }
}

package com.rupeek.hotel.payment.service.processor;

import com.rupeek.hotel.common.exception.InvalidRequestException;

import java.util.Map;
import java.util.regex.Pattern;

final class PaymentDetails {

    private PaymentDetails() {
    }

    static String require(Map<String, String> details, String field, Pattern format) {
        String value = details.get(field);
        if (value == null || !format.matcher(value).matches()) {
            throw new InvalidRequestException("Missing or invalid payment detail: " + field);
        }
        return value;
    }
}

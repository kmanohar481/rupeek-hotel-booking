package com.rupeek.hotel.payment.gateway;

public record GatewayResponse(boolean success, String reference, String message) {
}

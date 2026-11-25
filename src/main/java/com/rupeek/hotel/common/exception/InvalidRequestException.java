package com.rupeek.hotel.common.exception;

/**
 * Input breaks a business rule (bad dates, too many guests...). Mapped to 400.
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}

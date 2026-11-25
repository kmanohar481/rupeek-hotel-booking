package com.rupeek.hotel.common.exception;

/**
 * Requested resource does not exist. Mapped to 404.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}

package com.rupeek.hotel.common.exception;

/**
 * No inventory left for the requested room type and dates. Mapped to 409.
 */
public class RoomNotAvailableException extends RuntimeException {

    public RoomNotAvailableException(String message) {
        super(message);
    }
}

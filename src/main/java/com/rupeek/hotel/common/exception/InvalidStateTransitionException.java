package com.rupeek.hotel.common.exception;

/**
 * A booking was asked to move to a state its lifecycle does not allow. Mapped to 409.
 */
public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(String message) {
        super(message);
    }
}

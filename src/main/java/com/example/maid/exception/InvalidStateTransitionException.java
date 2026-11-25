package com.example.maid.exception;

public class InvalidStateTransitionException extends MaidBookingException {
    public InvalidStateTransitionException(String message) {
        super(message);
    }
}

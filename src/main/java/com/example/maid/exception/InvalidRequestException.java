package com.example.maid.exception;

public class InvalidRequestException extends MaidBookingException {
    public InvalidRequestException(String message) {
        super(message);
    }
}

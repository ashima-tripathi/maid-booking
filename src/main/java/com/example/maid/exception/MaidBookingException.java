package com.example.maid.exception;

/** Root of all business-rule failures raised by the domain and service layers. */
public class MaidBookingException extends RuntimeException {
    public MaidBookingException(String message) {
        super(message);
    }
}

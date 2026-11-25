package com.example.maid.exception;

public class PaymentFailedException extends MaidBookingException {
    public PaymentFailedException(String message) {
        super(message);
    }
}

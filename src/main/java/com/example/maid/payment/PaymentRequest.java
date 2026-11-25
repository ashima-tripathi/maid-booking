package com.example.maid.payment;

import com.example.maid.domain.PaymentMethodType;

/** instrument is the card number, UPI id or wallet id, depending on the method. */
public record PaymentRequest(String bookingId, String occurrenceId, PaymentMethodType method,
                             String instrument, String idempotencyKey) {
}

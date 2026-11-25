package com.example.maid.api.dto;

import com.example.maid.domain.Payment;
import com.example.maid.domain.PaymentMethodType;
import com.example.maid.domain.PaymentStatus;

import java.math.BigDecimal;

public record PaymentResponse(String id, String occurrenceId, BigDecimal amount, PaymentMethodType method,
                              PaymentStatus status, String transactionRef, BigDecimal refunded) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(payment.id(), payment.occurrenceId(), payment.amount().amount(),
                payment.method(), payment.status(), payment.transactionRef(), payment.refunded().amount());
    }
}

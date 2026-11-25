package com.example.maid.payment;

import com.example.maid.domain.PaymentMethodType;

public class CardPaymentProcessor extends SimulatedPaymentProcessor {

    public CardPaymentProcessor() {
        super("CARD");
    }

    @Override
    public PaymentMethodType method() {
        return PaymentMethodType.CARD;
    }

    @Override
    protected boolean isValidInstrument(String instrument) {
        return instrument.matches("\\d{16}");
    }
}

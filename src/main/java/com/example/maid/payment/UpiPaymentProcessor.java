package com.example.maid.payment;

import com.example.maid.domain.PaymentMethodType;

public class UpiPaymentProcessor extends SimulatedPaymentProcessor {

    public UpiPaymentProcessor() {
        super("UPI");
    }

    @Override
    public PaymentMethodType method() {
        return PaymentMethodType.UPI;
    }

    @Override
    protected boolean isValidInstrument(String instrument) {
        return instrument.matches("[\\w.\\-]+@[\\w.\\-]+");
    }
}

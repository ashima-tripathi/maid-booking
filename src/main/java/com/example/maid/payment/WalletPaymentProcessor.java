package com.example.maid.payment;

import com.example.maid.domain.PaymentMethodType;

public class WalletPaymentProcessor extends SimulatedPaymentProcessor {

    public WalletPaymentProcessor() {
        super("WALLET");
    }

    @Override
    public PaymentMethodType method() {
        return PaymentMethodType.WALLET;
    }

    @Override
    protected boolean isValidInstrument(String instrument) {
        return !instrument.isBlank();
    }
}

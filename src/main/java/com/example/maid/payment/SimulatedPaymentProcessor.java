package com.example.maid.payment;

import com.example.maid.domain.Money;

import java.util.UUID;

/**
 * Stand-in for a real gateway. Succeeds for any valid instrument, and declines instruments containing
 * "fail" or ending in "0000" so failure paths can be exercised.
 */
public abstract class SimulatedPaymentProcessor implements PaymentProcessor {

    private final String referencePrefix;

    protected SimulatedPaymentProcessor(String referencePrefix) {
        this.referencePrefix = referencePrefix;
    }

    protected abstract boolean isValidInstrument(String instrument);

    @Override
    public GatewayResult charge(Money amount, String instrument, String idempotencyKey) {
        if (instrument == null || !isValidInstrument(instrument)) {
            return GatewayResult.failed("Invalid " + method() + " details");
        }
        if (instrument.toLowerCase().contains("fail") || instrument.endsWith("0000")) {
            return GatewayResult.failed(method() + " payment was declined");
        }
        return GatewayResult.ok(referencePrefix + "-" + UUID.randomUUID());
    }

    @Override
    public GatewayResult refund(String transactionRef, Money amount) {
        return GatewayResult.ok("RF-" + transactionRef);
    }
}

package com.example.maid.payment;

import com.example.maid.domain.Money;
import com.example.maid.domain.PaymentMethodType;

/**
 * Our own abstraction over a payment method / third-party gateway. A new method (net banking, BNPL)
 * is a new implementation registered as a bean - no existing class changes.
 */
public interface PaymentProcessor {

    PaymentMethodType method();

    GatewayResult charge(Money amount, String instrument, String idempotencyKey);

    GatewayResult refund(String transactionRef, Money amount);
}

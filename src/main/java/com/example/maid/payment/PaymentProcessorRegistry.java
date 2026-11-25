package com.example.maid.payment;

import com.example.maid.domain.PaymentMethodType;
import com.example.maid.exception.InvalidRequestException;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class PaymentProcessorRegistry {

    private final Map<PaymentMethodType, PaymentProcessor> processors = new EnumMap<>(PaymentMethodType.class);

    public PaymentProcessorRegistry(List<PaymentProcessor> processors) {
        processors.forEach(processor -> this.processors.put(processor.method(), processor));
    }

    public PaymentProcessor forMethod(PaymentMethodType method) {
        PaymentProcessor processor = method == null ? null : processors.get(method);
        if (processor == null) {
            throw new InvalidRequestException("Unsupported payment method: " + method);
        }
        return processor;
    }
}

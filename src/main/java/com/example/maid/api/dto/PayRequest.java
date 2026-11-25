package com.example.maid.api.dto;

import com.example.maid.domain.PaymentMethodType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** instrument = card number, UPI id or wallet id depending on method. */
public record PayRequest(@NotNull PaymentMethodType method, @NotBlank String instrument,
                         @NotBlank String idempotencyKey) {
}

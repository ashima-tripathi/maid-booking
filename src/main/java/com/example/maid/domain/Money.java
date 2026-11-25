package com.example.maid.domain;

import com.example.maid.exception.InvalidRequestException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Objects;

/** Non-negative monetary amount, always normalised to two decimal places. */
public record Money(BigDecimal amount) implements Comparable<Money> {

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public Money {
        Objects.requireNonNull(amount, "amount");
        if (amount.signum() < 0) {
            throw new InvalidRequestException("Money cannot be negative");
        }
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money of(String value) {
        return new Money(new BigDecimal(value));
    }

    public static Money of(double value) {
        return new Money(BigDecimal.valueOf(value));
    }

    public Money percent(int percent) {
        return new Money(amount.multiply(BigDecimal.valueOf(percent)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
    }

    /** Treats this amount as an hourly rate and prices the given duration. */
    public Money forDuration(Duration duration) {
        BigDecimal minutes = BigDecimal.valueOf(duration.toMinutes());
        return new Money(amount.multiply(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP));
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    @Override
    public int compareTo(Money other) {
        return amount.compareTo(other.amount);
    }
}

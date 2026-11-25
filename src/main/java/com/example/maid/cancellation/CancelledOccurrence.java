package com.example.maid.cancellation;

import com.example.maid.domain.Money;

public record CancelledOccurrence(String occurrenceId, Money refund) {
}

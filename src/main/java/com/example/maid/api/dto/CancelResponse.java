package com.example.maid.api.dto;

import com.example.maid.cancellation.CancelledOccurrence;

import java.math.BigDecimal;
import java.util.List;

public record CancelResponse(List<Item> cancelled) {

    public record Item(String occurrenceId, BigDecimal refund) {
    }

    public static CancelResponse from(List<CancelledOccurrence> cancelled) {
        return new CancelResponse(cancelled.stream()
                .map(c -> new Item(c.occurrenceId(), c.refund().amount()))
                .toList());
    }
}

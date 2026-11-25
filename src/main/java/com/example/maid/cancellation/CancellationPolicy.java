package com.example.maid.cancellation;

import com.example.maid.domain.Occurrence;

import java.time.Instant;

/** Pluggable refund rule. Swap the implementation to change business policy. */
public interface CancellationPolicy {
    RefundDecision evaluate(Occurrence occurrence, Instant now);
}

package com.example.maid.cancellation;

import com.example.maid.domain.Occurrence;

import java.time.Duration;
import java.time.Instant;

/** 24h or more before the start: full refund. 2-24h: half. Under 2h: none. */
public class TimeBasedCancellationPolicy implements CancellationPolicy {

    private static final Duration FULL_REFUND_NOTICE = Duration.ofHours(24);
    private static final Duration PARTIAL_REFUND_NOTICE = Duration.ofHours(2);

    @Override
    public RefundDecision evaluate(Occurrence occurrence, Instant now) {
        Duration notice = Duration.between(now, occurrence.slot().start());
        if (notice.compareTo(FULL_REFUND_NOTICE) >= 0) {
            return new RefundDecision(100);
        }
        if (notice.compareTo(PARTIAL_REFUND_NOTICE) >= 0) {
            return new RefundDecision(50);
        }
        return new RefundDecision(0);
    }
}

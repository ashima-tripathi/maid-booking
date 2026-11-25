package com.example.maid.cancellation;

import com.example.maid.exception.InvalidRequestException;

/** Share of the amount paid that is returned to the customer, 0-100. */
public record RefundDecision(int percent) {

    public RefundDecision {
        if (percent < 0 || percent > 100) {
            throw new InvalidRequestException("Refund percent must be between 0 and 100");
        }
    }
}

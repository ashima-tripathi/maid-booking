package com.example.maid.pricing;

import com.example.maid.domain.Maid;
import com.example.maid.domain.Money;
import com.example.maid.domain.TimeRange;

/** Seam for dynamic pricing (peak hours, surge, discounts) without touching booking logic. */
public interface PricingStrategy {
    Money priceFor(Maid maid, TimeRange slot);
}

package com.example.maid.pricing;

import com.example.maid.domain.Maid;
import com.example.maid.domain.Money;
import com.example.maid.domain.TimeRange;

public class HourlyRatePricing implements PricingStrategy {

    @Override
    public Money priceFor(Maid maid, TimeRange slot) {
        return maid.hourlyRate().forDuration(slot.duration());
    }
}

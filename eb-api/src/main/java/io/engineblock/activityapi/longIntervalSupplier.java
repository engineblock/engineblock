package io.engineblock.activityapi;

import java.util.function.LongSupplier;

public interface longIntervalSupplier extends LongSupplier {
    /**
     * Get the next interval to be consumed by the caller, where the
     * first value is the returned value, and the last value is
     * one less than the first value plus the stride.
     * @param stride How many values to request
     * @return the base value of the interval to consume
     */
    long getInterval(long stride);
}

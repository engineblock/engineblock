package io.engineblock.activityapi;

public interface longIntervalSupplier {
    /**
     * Get the next interval to be consumed by the caller, where the
     * first value is the returned value, and the last value is
     * one less than the first value plus the stride.
     * @param stride How many values to request
     * @return the base value of the interval to consume
     */
    long getCycleInterval(int stride);
}

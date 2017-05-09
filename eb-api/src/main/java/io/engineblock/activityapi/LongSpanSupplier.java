package io.engineblock.activityapi;

import java.util.function.LongSupplier;

public interface LongSpanSupplier extends LongSupplier {
    long getSpan(long span);
}

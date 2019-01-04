package io.engineblock.activities.diag;

import io.engineblock.activityapi.core.ops.fluent.opcontext.BaseOpContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongToIntFunction;

public class DiagOpContext extends BaseOpContext {
    private String description;
    private List<String> diaglog = new ArrayList<>();

    private LongToIntFunction resultFunc;
    private long simulatedDelayNanos;

    public DiagOpContext(String description) {
        this.description = description;
    }

    /**
     * If this function is provided, the result will be set to the value of the
     * evaluted function with the op cycle.
     *
     * This is known as "resultfunc" in parameter space.
     *
     * The function must be thread-safe.
     *
     * @param resultFunc A function to map the cycle to the result value
     * @return this, for method chaining
     */
    public DiagOpContext withResultFunction(LongToIntFunction resultFunc) {
        this.resultFunc = resultFunc;
        return this;
    }

    /**
     * If this function is provided, the completion of the operation will be
     * delayed until the system nanotime is at least the op start time in
     * addition to the provided delay.
     *
     * This is controlled as "delayfunc" in parameter space.
     *
     * @param simulatedDelayNanos The amount of nanos ensure as a minimum
     *                            of processing time for this op
     */
    public DiagOpContext setSimulatedDelayNanos(long simulatedDelayNanos) {
        this.simulatedDelayNanos = simulatedDelayNanos;
        return this;
    }

    @Override
    public String toString() {
        return super.toString() + ", description:'" + description;
    }
    public String getDescription() {
        return description;
    }
    public void log(String logline) {
        this.diaglog.add(logline);
    }
    public List<String> getDiagLog() {
        return diaglog;
    }
    public long getSimulatedCompletionTime() {
        return this.getStartedAtNanos() + simulatedDelayNanos;
    }

}

package io.engineblock.activityimpl;

import io.engineblock.activityapi.core.ActionDispenser;
import io.engineblock.activityapi.core.Activity;
import io.engineblock.activityapi.core.MotorDispenser;
import io.engineblock.activityapi.core.RunState;
import io.engineblock.activityapi.cyclelog.filters.IntPredicateDispenser;
import io.engineblock.activityapi.output.OutputDispenser;
import io.engineblock.activityapi.input.InputDispenser;
import io.engineblock.rates.CoreRateLimiter;
import io.engineblock.rates.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * A default implementation of an Activity, suitable for building upon.
 */
public class SimpleActivity implements Activity {
    private final static Logger logger = LoggerFactory.getLogger(SimpleActivity.class);


    private List<AutoCloseable> closeables = new ArrayList<>();

    private MotorDispenser motorDispenser;
    private InputDispenser inputDispenser;
    private ActionDispenser actionDispenser;
    private OutputDispenser markerDispenser;
    private IntPredicateDispenser resultFilterDispenser;
    protected ActivityDef activityDef;
    private RunState runState = RunState.Uninitialized;
    private RateLimiter strideRateLimiter;
    private RateLimiter cycleRateLimiter;
    private RateLimiter phaseRateLimiter;

    public SimpleActivity(ActivityDef activityDef) {
        this.activityDef = activityDef;
    }

    public SimpleActivity(String activityDefString) {
        this(ActivityDef.parseActivityDef(activityDefString));
    }

    public synchronized RunState getRunState() {
        return runState;
    }

    public synchronized void setRunState(RunState runState) {
        this.runState = runState;
    }

    @Override
    public final MotorDispenser getMotorDispenserDelegate() {
        return motorDispenser;
    }

    @Override
    public final void setMotorDispenserDelegate(MotorDispenser motorDispenser) {
        this.motorDispenser = motorDispenser;
    }

    @Override
    public final InputDispenser getInputDispenserDelegate() {
        return inputDispenser;
    }

    @Override
    public final void setInputDispenserDelegate(InputDispenser inputDispenser) {
        this.inputDispenser = inputDispenser;
    }

    @Override
    public final ActionDispenser getActionDispenserDelegate() {
        return actionDispenser;
    }

    @Override
    public final void setActionDispenserDelegate(ActionDispenser actionDispenser) {
        this.actionDispenser = actionDispenser;
    }

    @Override
    public IntPredicateDispenser getResultFilterDispenserDelegate() {
        return resultFilterDispenser;
    }

    @Override
    public void setResultFilterDispenserDelegate(IntPredicateDispenser resultFilterDispenser) {
        this.resultFilterDispenser = resultFilterDispenser;
    }

    @Override
    public OutputDispenser getMarkerDispenserDelegate() {
        return this.markerDispenser;
    }

    @Override
    public void setOutputDispenserDelegate(OutputDispenser outputDispenser) {
        this.markerDispenser = outputDispenser;
    }

    @Override
    public ActivityDef getActivityDef() {
        return activityDef;
    }

    public String toString() {
        return getAlias();
    }

    @Override
    public int compareTo(Activity o) {
        return getAlias().compareTo(o.getAlias());
    }

    @Override
    public void registerAutoCloseable(AutoCloseable closeable) {
        this.closeables.add(closeable);
    }

    @Override
    public void closeAutoCloseables() {
        for (AutoCloseable closeable : closeables) {
            try {
                closeable.close();
            } catch (Exception e) {
                throw new RuntimeException("Error closing " + closeable);
            }
        }
        closeables.clear();
    }

    @Override
    public RateLimiter getCycleRateLimiter() {
        return this.cycleRateLimiter;
    }

    @Override
    public synchronized void setCycleRateLimiter(RateLimiter rateLimiter) {
        this.cycleRateLimiter = rateLimiter;
    }

    @Override
    public synchronized RateLimiter getCycleRateLimiter(Supplier<? extends RateLimiter> s) {
        if (cycleRateLimiter == null) {
            cycleRateLimiter = s.get();
        }
        return cycleRateLimiter;
    }

    @Override
    public synchronized RateLimiter getStrideRateLimiter() {
        return this.strideRateLimiter;
    }

    @Override
    public synchronized void setStrideRateLimiter(RateLimiter rateLimiter) {
        this.strideRateLimiter = rateLimiter;
    }

    @Override
    public synchronized RateLimiter getStrideRateLimiter(Supplier<? extends RateLimiter> s) {
        if (strideRateLimiter == null) {
            strideRateLimiter = s.get();
        }
        return strideRateLimiter;
    }

    @Override
    public synchronized void onActivityDefUpdate(ActivityDef activityDef) {

        activityDef.getParams().getOptionalDouble("targetrate").ifPresent(
                cycleRateLimit -> {
                    if (cycleRateLimiter == null) {
                        logger.debug("setting new cycle target rate to " + cycleRateLimit);
                        this.cycleRateLimiter = new CoreRateLimiter(cycleRateLimit);
                    } else {
                        if (cycleRateLimiter.getRate() != cycleRateLimit) {
                            logger.debug("adjusting cycle target rate to " + cycleRateLimit);
                            cycleRateLimiter.setRate(cycleRateLimit);
                        }
                    }
                }
        );

        activityDef.getParams().getOptionalDouble("striderate").ifPresent(
                strideRateLimit -> {
                    if (strideRateLimiter == null) {
                        logger.debug("setting new stride target rate to " + strideRateLimit);
                        this.strideRateLimiter = new CoreRateLimiter(strideRateLimit);
                    } else {
                        if (strideRateLimiter.getRate() != strideRateLimit) {
                            logger.debug("adjusting stride target rate to " + strideRateLimit);
                            strideRateLimiter.setRate(strideRateLimit);
                        }
                    }
                }
        );

        activityDef.getParams().getOptionalDouble("phaserate").ifPresent(
                phaseRateLimit -> {
                    if (phaseRateLimiter == null) {
                        logger.debug("setting new phase target rate to " + phaseRateLimit);
                        this.phaseRateLimiter = new CoreRateLimiter(phaseRateLimit);
                    } else {
                        if (phaseRateLimiter.getRate() != phaseRateLimit) {
                            logger.debug("adjusting phase target rate to " + phaseRateLimit);
                            phaseRateLimiter.setRate(phaseRateLimit);
                        }
                    }
                }
        );

    }

}

/*
 *
 *    Copyright 2016 jshook
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 * /
 */

package io.engineblock.activityapi.core;

import io.engineblock.activityapi.cyclelog.filters.IntPredicateDispenser;
import io.engineblock.activityapi.input.InputDispenser;
import io.engineblock.activityapi.output.OutputDispenser;
import io.engineblock.activityimpl.ActivityDef;
import io.engineblock.activityimpl.ParameterMap;
import io.engineblock.activityimpl.SimpleActivity;
import io.engineblock.activityapi.rates.RateLimiter;

import java.util.function.Supplier;

/**
 * Provides the components needed to build and run an activity a runtime.
 * The easiest way to build a useful Activity is to extend {@link SimpleActivity}.
 */
public interface Activity extends Comparable<Activity>, ActivityDefObserver {

    /**
     * Register an object which should be closed after this activity is shutdown.
     *
     * @param closeable An Autocloseable object
     */
    void registerAutoCloseable(AutoCloseable closeable);

    /**
     * Close all autocloseables that have been registered with this Activity.
     */
    void closeAutoCloseables();

    MotorDispenser getMotorDispenserDelegate();

    void setMotorDispenserDelegate(MotorDispenser motorDispenser);

    InputDispenser getInputDispenserDelegate();

    void setInputDispenserDelegate(InputDispenser inputDispenser);

    ActionDispenser getActionDispenserDelegate();

    void setActionDispenserDelegate(ActionDispenser actionDispenser);

    IntPredicateDispenser getResultFilterDispenserDelegate();

    void setResultFilterDispenserDelegate(IntPredicateDispenser resultFilterDispenser);

    OutputDispenser getMarkerDispenserDelegate();

    void setOutputDispenserDelegate(OutputDispenser outputDispenser);

    ActivityDef getActivityDef();

    default String getAlias() {
        return getActivityDef().getAlias();
    }

    default ParameterMap getParams() {
        return getActivityDef().getParams();
    }

    default void initActivity() {
    }

    RunState getRunState();
    void setRunState(RunState runState);

    default void shutdownActivity() {
    }

    default String getCycleSummary() {
        return getActivityDef().getCycleSummary();
    }

    /**
     * Get the current cycle rate limiter for this activity.
     * The cycle rate limiter is used to throttle the rate at which
     * cycles are dispatched across all threads in the activity
     * @return the cycle {@link RateLimiter}
     */
    RateLimiter getCycleLimiter();

    /**
     * Set the cycle rate limiter for this activity. This method should only
     * be used in a non-concurrent context. Otherwise, the supplier version
     * {@link #getCycleRateLimiter(Supplier)} should be used.
     * @param rateLimiter The cycle {@link RateLimiter} for this activity
     */
    void setCycleLimiter(RateLimiter rateLimiter);

    /**
     * Get or create the cycle rate limiter in a safe way. Implementations
     * should ensure that this method is synchronized or that each requestor
     * gets the same cycle rate limiter for the activity.
     * @param supplier A {@link RateLimiter} {@link Supplier}
     * @return An extant or newly created cycle {@link RateLimiter}
     */
    RateLimiter getCycleRateLimiter(Supplier<? extends RateLimiter> supplier);

    /**
     * Get the current stride rate limiter for this activity.
     * The stride rate limiter is used to throttle the rate at which
     * new strides are dispatched across all threads in an activity.
     * @return The stride {@link RateLimiter}
     */
    RateLimiter getStrideLimiter();

    /**
     * Set the stride rate limiter for this activity. This method should only
     * be used in a non-concurrent context. Otherwise, the supplier version
     * {@link #getStrideRateLimiter(Supplier)}} should be used.
     * @param rateLimiter The stride {@link RateLimiter} for this activity.
     */
    void setStrideLimiter(RateLimiter rateLimiter);

    /**
     * Get or create the stride {@link RateLimiter} in a concurrent-safe
     * way. Implementations should ensure that this method is synchronized or
     * that each requestor gets the same stride rate limiter for the activity.
     * @param supplier A {@link RateLimiter} {@link Supplier}
     * @return An extant or newly created stride {@link RateLimiter}
     */
    RateLimiter getStrideRateLimiter(Supplier<? extends RateLimiter> supplier);

    /**
     * Get the current phase rate limiter for this activity.
     * The phase rate limiter is used to throttle the rate at which
     * new phases are dispatched across all threads in an activity.
     * @return The stride {@link RateLimiter}
     */
    RateLimiter getPhaseLimiter();

    /**
     * Set the phase rate limiter for this activity. This method should only
     * be used in a non-concurrent context. Otherwise, the supplier version
     * {@link #getPhaseRateLimiter(Supplier)}} should be used.
     * @param rateLimiter The phase {@link RateLimiter} for this activity.
     */
    void setPhaseLimiter(RateLimiter rateLimiter);

    /**
     * Get or create the phase {@link RateLimiter} in a concurrent-safe
     * way. Implementations should ensure that this method is synchronized or
     * that each requestor gets the same phase rate limiter for the activity.
     * @param supplier A {@link RateLimiter} {@link Supplier}
     * @return An extant or newly created phase {@link RateLimiter}
     */
    RateLimiter getPhaseRateLimiter(Supplier<? extends RateLimiter> supplier);

}

/*
 *   Copyright 2016 jshook
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package io.engineblock.activities.diag;

import com.codahale.metrics.Histogram;
import io.engineblock.activityapi.core.Activity;
import io.engineblock.activityapi.core.ActivityDefObserver;
import io.engineblock.activityimpl.ActivityDef;
import io.engineblock.activityimpl.SimpleActivity;
import io.engineblock.metrics.ActivityMetrics;
import io.engineblock.activityapi.rates.RateLimiter;
import io.engineblock.activityapi.rates.RateLimiters;
import io.engineblock.activityapi.rates.RateSpec;

public class DiagActivity extends SimpleActivity implements Activity, ActivityDefObserver {

    protected Histogram delayHistogram;
    private RateLimiter diagRateLimiter;

    public DiagActivity(ActivityDef activityDef) {
        super(activityDef);
    }

    @Override
    public void initActivity() {
        delayHistogram = ActivityMetrics.histogram(activityDef, "delay");
        Integer initdelay = activityDef.getParams().getOptionalInteger("initdelay").orElse(0);
        try {
            Thread.sleep(initdelay);
        } catch (InterruptedException ignored) {
        }
    }

    public RateLimiter getDiagRateLimiter() {
        return diagRateLimiter;
    }

    @Override
    public synchronized void onActivityDefUpdate(ActivityDef activityDef) {
        super.onActivityDefUpdate(activityDef);

        activityDef.getParams()
                .getOptionalString("diagrate")
                .map(RateSpec::new)
                .ifPresent(spec -> diagRateLimiter= RateLimiters.createOrUpdate(getActivityDef(),"diag",diagRateLimiter,spec));
    }
}

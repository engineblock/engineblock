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

package io.engineblock.extensions.optimizers;

import com.codahale.metrics.MetricRegistry;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.apache.commons.math3.optim.*;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.BOBYQAOptimizer;
import org.slf4j.Logger;

import javax.script.ScriptContext;
import java.util.Arrays;
import java.util.List;

public class BobyqaOptimizerInstance {

    private final Logger logger;
    private final MetricRegistry metricRegistry;
    private final ScriptContext scriptContext;

    private int numberOfInterpolationPoints = 9;
    private double initialTrustRegionRadius = Double.MAX_VALUE;
    private double stoppingTrustRegionRadius = 1.0D;

    private int varcount;
    private MultivariateDynamicScript advancingScriptObject;
    private MultivariateDynamicScript objectiveScriptObject;
    private SimpleBounds bounds;
    private InitialGuess initialGuess;
    private PointValuePair result;
    private int maxEval;

    public BobyqaOptimizerInstance(Logger logger, MetricRegistry metricRegistry, ScriptContext scriptContext) {
        this.logger = logger;
        this.metricRegistry = metricRegistry;
        this.scriptContext = scriptContext;
    }

    public BobyqaOptimizerInstance setPoints(int numberOfInterpolationPoints) {
        this.numberOfInterpolationPoints = numberOfInterpolationPoints;
        return this;
    }
    public BobyqaOptimizerInstance setInitialRadius(double initialTrustRegionRadius) {
        this.initialTrustRegionRadius = initialTrustRegionRadius;
        return this;
    }
    public BobyqaOptimizerInstance setStoppingRadius(double stoppingTrustRegionRadius) {
        this.stoppingTrustRegionRadius=stoppingTrustRegionRadius;
        return this;
    }

    public BobyqaOptimizerInstance setSteppingFunction(int varcount, Object f) {
        if (f instanceof ScriptObjectMirror) {
            ScriptObjectMirror scriptObject = (ScriptObjectMirror) f;
            if (!scriptObject.isFunction()) {
                throw new RuntimeException("Unable to setFunction with a non-function object");
            }
            this.advancingScriptObject = new MultivariateDynamicScript(logger,varcount, scriptObject);
        }
        return this;
    }

    public BobyqaOptimizerInstance setBounds(double... values) {
        double[] bottom = Arrays.copyOfRange(values, 0, values.length >> 1);
        double[] top = Arrays.copyOfRange(values, values.length >> 1, values.length);
        this.bounds = new SimpleBounds(bottom, top);
        return this;
    }

    public BobyqaOptimizerInstance setObjectiveFunction(int varcount, Object f) {
        if (f instanceof ScriptObjectMirror) {
            ScriptObjectMirror scriptObject = (ScriptObjectMirror) f;
            if (!scriptObject.isFunction()) {
                throw new RuntimeException("Unable to setFunction with a non-function object");
            }
            this.objectiveScriptObject = new MultivariateDynamicScript(logger,varcount, scriptObject);
        }
        return this;
    }

    public BobyqaOptimizerInstance setMaxEval(int maxEval) {
        this.maxEval = maxEval;
        return this;
    }
    public double[] optimize(double[] initialGuess) {
        this.initialGuess = new InitialGuess(initialGuess);

        BOBYQAOptimizer mo = new BOBYQAOptimizer(
                this.numberOfInterpolationPoints,
                this.initialTrustRegionRadius,
                this.stoppingTrustRegionRadius
        );
        List<OptimizationData> od = List.of(
                new ObjectiveFunction(this.objectiveScriptObject),
                GoalType.MAXIMIZE,
                this.initialGuess,
                new MaxEval(this.maxEval),
                this.bounds
        );
        this.result = mo.optimize(od.toArray(new OptimizationData[0]));
        return getResult();
    }

    public double[] getResult() {
        return result.getPoint();
    }
}

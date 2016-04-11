package com.metawiring.tools.activityapi;

public interface MotorDispenserProvider {
    public MotorDispenser getMotorDispenser(ActivityDef activityDef, InputDispenser inputDispenser, ActionDispenser actionDispenser);
}

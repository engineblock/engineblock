
activitydef = {
    "alias" : "ratelimited",
    "type" : "diag",
    "cycles" : "600",
    "threads" : "10",
    "targetrate" : "200.0"
};

scenario.start(activitydef);

while (scenario.isRunningActivity(activitydef)) {
    achievedRate = metrics.ratelimited.timer.meanRate;
    currentCycle = metrics.ratelimited.timer.count;
    print("currentCycle = " + currentCycle + ", mean rate = " + achievedRate);
    scenario.waitMillis(100);
}




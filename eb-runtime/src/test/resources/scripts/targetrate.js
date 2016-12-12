
activitydef = {
    "alias" : "ratelimited",
    "type" : "diag",
    "cycles" : "300",
    "threads" : "10",
    "targetrate" : "1K"
};

scenario.start(activitydef);

while (scenario.isRunningActivity(activitydef)) {
    achievedRate = metrics.ratelimited.cycles.meanRate;
    currentCycle = metrics.ratelimited.cycles.count;
}
print("currentCycle = " + currentCycle);
print("mean rate = " + achievedRate);




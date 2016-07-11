
activitydef = {
    "alias" : "ratelimited",
    "type" : "diag",
    "cycles" : "600",
    "threads" : "10",
    "targetrate" : "200.0"
};

scenario.start(activitydef);

while (scenario.isRunningActivity(activitydef)) {
    achievedRate = metrics.get("activity.ratelimited.timer").getMeanRate();
    currentCycle = metrics.get("activity.ratelimited.timer").getCount();
    print("currentCycle = " + currentCycle + ", mean rate = " + achievedRate);
    scenario.waitMillis(100);
}




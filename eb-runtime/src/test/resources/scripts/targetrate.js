
activitydef = {
    "alias" : "ratelimited",
    "type" : "diag",
    "cycles" : "1K",
    "threads" : "10",
    "targetrate" : "1K",
    "phases" : 15
};

scenario.run(activitydef);

print("current cycle = " + metrics.ratelimited.cycles.count);
print("mean cycle rate = " + metrics.ratelimited.cycles.meanRate);
print("current phase = " + metrics.ratelimited.phases.count);
print("mean phase rate = " + metrics.ratelimited.phases.meanRate);




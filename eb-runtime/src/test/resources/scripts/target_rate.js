
activitydef = {
    "alias" : "target_rate",
    "type" : "diag",
    "cycles" : "1K",
    "threads" : "10",
    "targetrate" : "1K",
    "phases" : 15
};

scenario.run(activitydef);

print("current cycle = " + metrics.target_rate.cycles.count);
print("mean cycle rate = " + metrics.target_rate.cycles.meanRate);
print("current phase = " + metrics.target_rate.phases.count);
print("mean phase rate = " + metrics.target_rate.phases.meanRate);




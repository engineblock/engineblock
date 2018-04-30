co_cycle_delay = {
    "alias" : "co_cycle_delay",
    "type" : "diag",
    "diagrate" : "800",
    "cycles" : "0..10000",
    "threads" : "1",
    "cocyclerate" : "1000,1.2,co"
};

print('starting activity co_cycle_delay');
scenario.start(co_cycle_delay);
scenario.waitMillis(4000);
print('step1 metrics.co_cycle_delay=' + metrics.co_cycle_delay.cycles.count);
activities.co_cycle_delay.diagrate="1100";
scenario.waitMillis(4000);
print('step2 metrics.co_cycle_delay=' + metrics.co_cycle_delay.cycles.count);
scenario.awaitActivity("co_cycle_delay");
print("awaited activity");
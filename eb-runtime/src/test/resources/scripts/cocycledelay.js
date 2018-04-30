co_cycle_delay = {
    "alias" : "co_cycle_delay",
    "type" : "diag",
    "diagrate" : "800",
    "cycles" : "0..10000",
    "threads" : "1",
    "co_cyclerate" : "1000"
};

print('starting activity co_cycle_delay');
scenario.start(co_cycle_delay);
scenario.waitMillis(4000);
print('step1 metrics.co_cycle_delay=' + metrics.co_cycle_delay.cycle.cco_delay_gauge.value);
activities.co_cycle_delay.diagrate="10000";
scenario.awaitActivity("co_cycle_delay");
print('step2 metrics.co_cycle_delay=' + metrics.co_cycle_delay.cycle.cco_delay_gauge.value);
print("awaited activity");
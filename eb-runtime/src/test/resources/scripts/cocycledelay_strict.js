co_cycle_delay = {
    "alias" : "co_cycle_delay",
    "type" : "diag",
    "diagrate" : "800",
    "cycles" : "0..10000",
    "threads" : "1",
    "cyclerate" : "1000,1.0,hybrid"
};

print('starting activity co_cycle_delay');
scenario.start(co_cycle_delay);
scenario.waitMillis(4000);
print('step1 metrics.waittime=' + metrics.co_cycle_delay.cycle.waittime.value);
activities.co_cycle_delay.diagrate="10000";
for(i=0;i<5;i++) {
    if (! scenario.isRunningActivity('co_cycle_delay')) {
        print("scenario exited prematurely, aborting.");
        break;
    }
    print("iteration " + i + " waittime now " + metrics.co_cycle_delay.cycle.waittime.value);
    scenario.waitMillis(1000);
}
//scenario.awaitActivity("co_cycle_delay");
print('step2 metrics.waittime=' + metrics.co_cycle_delay.cycle.waittime.value);
print("awaited activity");
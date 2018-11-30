co_cycle_delay = {
    "alias": "co_cycle_delay",
    "type": "diag",
    "diagrate": "500",
    "cycles": "0..1000000",
    "threads": "10",
    "cyclerate": "1000,1.5,hybrid",
    "modulo": "1000"
};

print('starting activity co_cycle_delay');
scenario.start(co_cycle_delay);
for (i = 0; i < 5; i++) {
    scenario.waitMillis(1000);
    if (!scenario.isRunningActivity('co_cycle_delay')) {
        print("scenario exited prematurely, aborting.");
        break;
    }
    print("backlogging, cycles=" + metrics.co_cycle_delay.cycles.count +
        " waittime=" + metrics.co_cycle_delay.cycle.waittime.value +
        " diagrate=" + activities.co_cycle_delay.diagrate +
        " cyclerate=" + activities.co_cycle_delay.cyclerate
    );

    // print("config: " +
    //     " cycle.config_cyclerate=" + metrics.co_cycle_delay.cycle.config_cyclerate.value +
    //     " cycle.config_burstrate=" + metrics.co_cycle_delay.cycle.config_burstrate.value +
    //     " cycle.waittime=" + metrics.co_cycle_delay.cycle.waittime.value
    // );
    //
    // print("diag config: " +
    //     " diag.config_cyclerate=" + metrics.co_cycle_delay.diag.config_cyclerate.value +
    //     " diag.config_burstrate=" + metrics.co_cycle_delay.diag.config_burstrate.value +
    //     " diag.waittime=" + metrics.co_cycle_delay.diag.waittime.value
    // );

}
print('step1 metrics.waittime=' + metrics.co_cycle_delay.cycle.waittime.value);
activities.co_cycle_delay.diagrate = "10000";

for (i = 0; i < 10; i++) {
    if (!scenario.isRunningActivity('co_cycle_delay')) {
        print("scenario exited prematurely, aborting.");
        break;
    }
    print("recovering, cycles=" + metrics.co_cycle_delay.cycles.count +
        " waittime=" + metrics.co_cycle_delay.cycle.waittime.value +
        " diagrate=" + activities.co_cycle_delay.diagrate +
        " cyclerate=" + activities.co_cycle_delay.cyclerate
    );

    // print("cycle config: " +
    //     " cycle.config_cyclerate=" + metrics.co_cycle_delay.cycle.config_cyclerate.value +
    //     " cycle.config_burstrate=" + metrics.co_cycle_delay.cycle.config_burstrate.value +
    //     " cycle.waittime=" + metrics.co_cycle_delay.cycle.waittime.value
    // );
    //
    // print("diag config: " +
    //     " diag.config_cyclerate=" + metrics.co_cycle_delay.diag.config_cyclerate.value +
    //     " diag.config_burstrate=" + metrics.co_cycle_delay.diag.config_burstrate.value +
    //     " diag.waittime=" + metrics.co_cycle_delay.diag.waittime.value
    // );


    scenario.waitMillis(1000);
    if (metrics.co_cycle_delay.cycle.waittime.value == 0) {
        print("waittime trended to 0 as expected, exiting on iteration " + i);
        break;
    }
}
//scenario.awaitActivity("co_cycle_delay");
print('step2 metrics.waittime=' + metrics.co_cycle_delay.cycle.waittime.value);
scenario.stop(co_cycle_delay);
print("stopped activity co_cycle_delay");

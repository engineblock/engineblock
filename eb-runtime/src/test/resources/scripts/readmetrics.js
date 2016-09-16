activitydef = {
    "alias" : "testactivity",
    "type" : "diag",
    "cycles" : "0..1000000000",
    "threads" : "25",
    "interval" : "2000"
};
scenario.start(activitydef);

while (metrics.testactivity.cycles.count < 1000) {
    print('waiting 10ms because metrics.testactivity.cycles<10000 : ' + metrics.testactivity.cycles.count);
    scenario.waitMillis(10);

}
scenario.stop(activitydef);
print("count: " + metrics.testactivity.cycles.count);

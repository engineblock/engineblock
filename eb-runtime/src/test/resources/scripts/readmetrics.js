//logger.info("testing dynamically changing threads.");
activitydef = {
    "alias" : "testactivity",
    "type" : "diag",
    "cycles" : "1..1000000000",
    "threads" : "25",
    "interval" : "2000"
};

scenario.start(activitydef);
remaining=100;
while (!metrics.get("activity.testactivity.timer") && (remaining > 0)) {
    print('waiting 100ms for cycles to be present');
    scenario.waitMillis(100);
    remaining--;
}
while (metrics.get("activity.testactivity.timer").getCount() < 10000) {
    print('waiting 10ms because metrics.testactivity.cycles<10000 : ' + metrics.get("activity.testactivity.timer").getCount());
    scenario.waitMillis(10);

}
scenario.stop(activitydef);
print('stopping scenario because metrics.get("activity.testactivity.timer").getCount() == '
    + metrics.get("activity.testactivity.timer").getCount());

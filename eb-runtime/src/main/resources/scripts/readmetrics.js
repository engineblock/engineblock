//logger.info("testing dynamically changing threads.");
activitydef = {
    "alias" : "testactivity",
    "type" : "diag",
    "cycles" : "1.1000000000",
    "threads" : "10",
    "interval" : "2000"
};

scenario.start(activitydef);

while (metrics.testactivity.cycles < 10000) {
    print('waiting 10ms because metrics.testactivity.cycles<10000 : ' + metrics.testactivity.cycles);
    scenario.waitMillis(10);
}
scenario.stop('diag');
print('stopping scenario because metrics.testactivity.cycles == ' + metrics.testactivity.cycles);

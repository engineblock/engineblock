var csvlogger = csvmetrics.log("csvmetricstestdir");

activitydef = {
    "alias" : "speedcheck",
    "type" : "diag",
    "cycles" : "50000",
    "threads" : "20",
    "interval" : "2000",
    "targetrate" : "10000.0"
};
scenario.start(activitydef);
csvlogger.add(metrics.speedcheck.cycles);

scenario.waitMillis(1000);
scenario.stop(activitydef);

csvlogger.report();

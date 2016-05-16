print('waiting 500 ms');
scenario.waitMillis(500);
print('waited');
scenario.start('type=diag;alias=test;cycles=1..10000000;threads=10;interval=2000;');
activities.test.interval=1000;
activities.test.threads=5;
print('waiting 5000 ms');
scenario.waitMillis(5000);
activities.test.threads=20;
//activities.test.interval=500;
print('waiting 5000 ms');
scenario.waitMillis(5000);





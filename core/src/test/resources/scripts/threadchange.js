print('waiting 500 ms');
sc.waitMillis(500);
print('waited');
sc.start('type=diag;alias=test;cycles=1..10000000;threads=10;interval=2000;');
activities.test.interval=1000;
activities.test.threads=5;
print('waiting 5000 ms');
sc.waitMillis(5000);
activities.test.threads=20;
activities.test.interval=500;




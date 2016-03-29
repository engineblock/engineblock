//var sc = new com.metawiring.load.cycler.ScenarioController();
print('waiting 500 ms');
sc.waitMillis(500);
print('waited');
sc.start('type=diag;alias=test;cycles=1..10000000;threads=10;interval=2000;');

activities.test.interval=1000;
activities.test.threads=1;
activities.test.threads=10;
activities.test.threads=20;
// TODO: Fix this: why only threads?
activities.test.threads=2;
//sc.modify('test','threads',"1");
print('waiting 5000 ms');
sc.waitMillis(5000);
activities.test.threads=20;
activities.test.interval=500;

////sc.modify('test','threads',"20");
//print('modified threads to 20');

//sc.modify('test','threads'    ,'15');
//sc.stop('test');




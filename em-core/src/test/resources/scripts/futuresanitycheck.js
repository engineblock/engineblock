//var sc = new com.metawiring.load.cycler.ScenarioController();
print('waiting 500 ms');
sc.waitMillis(500);
print('waited');
sc.start('type=diag;alias=test;cycles=1..1000000000;threads=10;interval=2000;');
print('waiting again');
sc.modify('test','threads',"1");
print('waiting 5000 ms');
sc.waitMillis(5000);
sc.modify('test','threads',"20");
print('modified threads to 20');

//sc.modify('test','threads','15');
//sc.stop('test');




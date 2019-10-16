var optimo = optimos.init();
optimo.setPoints(6).setInitialRadius(10000.0).setStoppingRadius(0.001);
optimo.setMaxEval(1000);
optimo.setBounds(0.0,0.0,200000.0,200000.0);
optimo.setObjectiveFunction(2,
    function(pa,pb) {
        var a = 0.0+pa;
        var b = 0.0+pb;
        var result= 1000000-((Math.abs(100-a) + Math.abs(100-b)));
        print("a=" +a+ ",b=" + b + ", r=" + result);
        return result;
    }
);
var result = optimo.optimize([1.0,1.0]);
print("optimized result was " + result);


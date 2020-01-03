var optimo = optimos.init();

optimo.param('pa', 0.0, 200000.0);
optimo.param('pb', 0.0, 200000.0);

optimo.setInitialRadius(10000.0).setStoppingRadius(0.001).setMaxEval(1000);

optimo.setObjectiveFunction(
    function (ary) {
        // var arraydata = Java.from(ary);
        // print("arraydata:" + JSON.stringify(arraydata));

        var a = 0.0 + ary.pa;
        var b = 0.0 + ary.pb;

        var result = 1000000 - ((Math.abs(100 - a) + Math.abs(100 - b)));
        print("a=" + a + ",b=" + b + ", r=" + result);
        return result;
    }
);

var result = optimo.optimize();

print("optimized result was " + result);
print("map of result was " + result.getVarMap());


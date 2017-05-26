### tracking

~~~ 
# time bin/eb run type=diag cycles=0..100000000 errormodulo=10 threads=24 tracking=a=b -vv
# Time to execute 100,000,000 diag loops with tracking enabled
real    0m25.213s // 2.6% longer
user    2m40.570s
sys     0m0.710s

# time bin/eb run type=diag cycles=0..100000000 errormodulo=10 threads=24 -vv
# Time to execute 100,000,000 diag loops without tracking enabled
real    0m24.559s
user    2m39.123s
sys     0m0.769s
~~~

### stride=42

A new general purpose activity parameter has been added which allows you to
group consecutive operations in an activity to be executed iteratively within a
specific thread. By default, stride=1, and the behavior is as before.

Activity types may wish to overide this to some value based on the most obvious
grouping of operations.

### type=csv and type=json

Two new activity types, "csv" and "json" will be added soon that can be used
for experimentation with data mapping, etc.
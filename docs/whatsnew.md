# What's New in EngineBlock

## Recent updates for 2017-05

### stride=42

A new general purpose activity parameter has been added which allows you to
group consecutive operations in an activity to be executed iteratively within a
specific thread. By default, stride=1, and the behavior is as before.

Activity types may wish to overide this to some value based on the most obvious
grouping of operations.

### type=csv and type=json

Two new activity types, "csv" and "json" will be added soon that can be used
for experimentation with data mapping, etc.

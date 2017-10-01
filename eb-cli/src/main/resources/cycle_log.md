## rlefile input and output type

### rlefile format

This file is a binary format that encodes ranges of cycles
in RLE interval form. This means that it will be relatively compact
for scenarios that have many repeats of the same result.

You can dump an rlefile to the screen to see the content in text form
by running a command like this:

    PROG --dump-cycle-log <filename>

### rlefile output

When you want an activity to record its per-cycle result for
later use, you can use an rlefile output. This is configured as:

    ... output=type:rlefile,file:somefile ...

#### output filtering

If you want to limit the cycles that are recorded in a cycle log,
you can add an inputlog parameter like this:

    .... outputfilter=range:5..10

This will cause the cycle log to contain only cycle ranges or cycles
which have results within the specified range, inclusive.

### rlefile output

When you want an

When you want a
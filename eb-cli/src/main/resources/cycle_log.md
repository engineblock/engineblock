## cycle logging and re-use

### cycle_log format

This file is a binary format that encodes ranges of cycles
in RLE interval form. This means that it will be relatively compact
for scenarios that have many repeats of the same result, as well
as low-overhead when running scenarios.

All cycle logfiles have the *.cyclelog* suffix.

You can dump an rlefile to the screen to see the content in text form
by running a command like this:

    PROG --dump-cycle-log <filename> [spans|cycles]

You do not need to specify the extension. If you do not specify either
optional format at the end, then *cycles* is assumed. It will print output like this:

    0->3
    1->3
    ...

Alternately, you can see the individual RLE spans with the *spans* format, which looks
like this:

    [0,100)->3
    ...

This format uses the '[x,y)' notation to remind you that the spans are all closed-open
intervals, including the starting cycle number but not the ending one.

### Using cycle logs as outputs

When you want an activity to record its per-cycle result for
later use, you can specify a cycle log as its output. This is configured as:

    ... output=type:cyclelog,file:somefile ...

### Using cycle logs as inputs

You can have all the cycles in a cycle log as the input cycles of an activity like this:

    ... input=type:cyclelog,file:somefile ...

Note, that when you use cycle logs as inputs, not all cycles are guaranteed to be
in order. In most cases, they will be, due to reordering support on RLE encoding. However,
that uses a sliding-window buffer, and in some cases RLE spans can occur out of
order in a cycle log.

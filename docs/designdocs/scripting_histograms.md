Scripting Histograms
====================

# Synopsis

Histogram-based analysis is one of the most effective methods for performance characterization. However, there is not enough flexibility in how the existing tools work to support the scripting needs in EngineBlock. This document outlines the problems with the existing tool in the context of advanced scripting and a design for how to collect and use histogram data effectively.

## The Accuracy Challenge

When making analysis decisions from histogram data, you need full delta snapshots between measurement times. Anything else serves to reduce the accuracy and timeliness of the histogram data. Time decaying reservoirs attempt to strike a balance between timeliness and accuracy, but always by throwing away some data. Resetting on snapshot reservoirs solve this problem by making the snapshots explicit to a time interval. However, they do not function well for multiple consumers, given that accessing the snapshot has the effect of taking the information out of the reservoir. This type of reservoir by-design supports only a single-consumer data flow.

## Requirement: No Surprises

When you are consuming data from a histogram/reservoir for both reporting and local calculations, you need to be able to see the same data in your snapshot as you do in your reported data. One way to do this would be to have multiple histograms for the same input data, accessing the one with the behavior you want. However, this is wasteful, and more confusing than it should be. It can lead to surprises in teh API, given that there is now more than one way to get effectively the same data.
  
As well, it makes sense to have some consumer be in charge of when the snapshots get reset internally, with the other consumers simply observing the same snapshots on access. The normal expectation is that previous usage patterns will continue to work as before (namely configuring graphite reporters to use resettingOnSnapshot histograms), and that new behavior will only occur when asked for. For this reason, any approach to sharing the snapshots between consumers should honor the previous usage patterns by default.

## An Approach

A histogram/reservoir will be provided that supports the previous resettingOnSnapshot behavior by default. If it is access locally via an attached metric API, then it will change its behavior slightly.

- A method will be provided to allow an *attached* metric to access and reset the snapshot. This method require a cache-time parameter which says how long to cache a snapshot for.
- If an attached metric does access and reset the snapshot, that snapshot will become the active reservoir for any subsequent snapshot calls to the original metric, as long as they occur within the cache-time.
- If any snapshot call to the original metric occurs after an attached metric call *and* after the cache-time expires, then the snapshot will be handled as a typical resettingOnSnapshot access, restoring the normal behavior.

The net effect of this is that you have the option of the *usual* behavior or the *new* behavior simply by attaching an additional metric view. If you stop using the new metric view, it reverts to the previous behavior, just with the current snapshot data.
 
When you are using the new behavior, it will be because:
1. You want the local (attached) metric view to control the timing of histogram snapshots. It is the primary consumer in this case, with any other (remote via reporting) providing a window into local decisions.
2. You do not want the remote view to have different data than you are using for local computations, because of 1)


# Status
This is in active development. Collaborators are welcome. However, there is still work to be done to groom the slope for new users.

## Engine Block

This project aims to provide a missing power tool in the test tooling arsenal.

The design goals:

1. Provide a useful and intuitive Reusable Machine Pattern for constructing and reasoning about concurrent performance tests. To encourage this, the runtime machinery is based on [simple and tangible core concepts](docs/core_concepts.md).
2. Reduce testing time of complex scenarios with many variables. This is achieved by controlling tests from an [open javascript sandbox](docs/scripting.md). This makes more sophisticated scenarios possible when needed.
3. Minimize the amount of effort required to get empirical results from a test cycle. For this, [metrics reporting](docs/metrics.md) is baked in.

In short, Engine Block wishes to be a programmable power tool for performance testing. However, it is somewhat generic. It doesn't know directly about a particular type of system, or protocol.
It simply provides a suitable machine harness in which to put your drivers and testing logic. If you know how to build a client for a particular kind of system, EB will let you load it like a plugin and control it dynamically.

The most direct way to do this, if you are a tool developer, is to build your own activity type drivers and embed EB as the core runtime. You can always experiment with it and learn how it works by using the built-in diagnostic drivers.

## Scale

For now, this is a single-instance client. For testing large clusters, you will still need to run multiple clients to provide adequate loading. Experience has shown that one client can adequately drive 3-5 target systems for typical workloads. As always, be sure to watch your metrics on both sides to ensure that your testing instrument isn't the thing being measured. A future enhancement may allow EB to be powered by multiple clients for greater load generating capacity.

## Getting Started

You can begin at [Quick Start](docs/quickstart.md) or consult the full [Usage Guide](docs/usage_guide.md). At least taking a glance at the guide is recommended.

## Contributions
If you are interested in contributing to Engine Block, more information is available in the [Developer's section](doc/developers.md).

## History

The Engine Block project started as a branch of [test client](http://github.com/jshook/testclient). It has since evolved to be more generic.

## License

This is licensed under the Apache Public License 2.0


![Travis CI Build Status](https://api.travis-ci.org/engineblock/engineblock.svg)


## The Empirical Machine

This project aims to provide a missing power tool in the test tooling arsenal.

The design goals and rationale:

1. Provide a useful and intuitive machine pattern for constructing and reasoning about concurrent performance tests. To encourage this, the core design elements  are [simple and tangible](docs/core_concepts.md).
2. Enable direct construction of contextual workloads. If you have a data model, example statements, and some statistics about your data, you should be able to put these into a simple configuration and run your test. This is encouraged as TestClient's [primary usage pattern]( https://raw.githubusercontent.com/jshook/testclient/master/src/main/resources/activities/write-telemetry.yaml).
3. Reduce testing time of complex scenarios with many variables. This is achieved by allowing tests to be dynamic within a scenario, supported by a scripting sandbox. This makes more sophisticated scenarios possible when needed.
4. Minimize the amount of effort required to get results from a test cycle. This is simply a matter of using the best tooling available to bring data together in a common view. In other words, metrics and dashboards. TestClient supports reporting to common metrics systems out of the box.

In short, TestClient wishes to be a programmable power tool for performance testing.


## Scale

For now, this is a single-instance client. For testing large clusters, you will still need to run multiple clients to provide adequate loading. Experience has shown that one client can adequately drive 3-5 target systems for typical workloads. As always, be sure to watch your metrics on both sides to ensure that your testing instrument isn't the thing being measured.

## Getting Started

You can begin at [Quick Start](docs/quickstart.md) or consult the full [Users Guide](docs/usersguide.md). At least taking a glance at the user guide is recommended.
 
If you are interested in contributing to TestClient, more information is available in the [Developer's section](doc/developers.md)
## License

This is licensed under the Apache Public License 2.0

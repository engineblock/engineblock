# TestClient Scripting

## Motive

The TestClient runtime is a combination of a scripting sandbox and a workload execution machine. This is not accidental.
With this particular arrangement, it should be possible to build sophisticated tests across a variety of scenarios. 
In particular, logic which can observe and react to the system under test can be powerful. It can make it possible
to break away from the conventional run-interpret-adjust cycle which is all too often done by human hands. The ability
to get a useful set of answers from nontrivial systems with reasonable effort requires a more sophisticated approach.

## Script Environment

The TestScript environment is the standard Nashorn Javascript environment, with slight modifications meant to streamline
understanding and usage. The modification are:

- Active Variables, control variables which, when assigned to, cause an immediate change in the testengine runtime. Each of the variables below is pre-wired into each script environment.
- - activities.<alias>.<paramname> - _read+write_ - Every parameter which is assigned to in this map will cause a synchronous notification to the execution runtime, including the respective activity (identified by <alias>) if it has been implemented to react.
- - metrics.<alias>.<metric> - _read-only_ - Every value available here is a metrics object from the internal instrumentation.
- - sc - _read+write_ - This is the __Scenario Controller__ object which manages the activity executors in the runtime.

Interaction with the TestClient runtime and the activities therein is made easy by the above variables and objects. You can make
use of more extensive Java or Javascript libraries as needed.

## Control Flow

When a script is run, it has absolute control over the scenario runtime while it is active. Once the script reaches its end, however, it
will only exit if all activities have completed. If you want to explicitly stop a script, you must stop all activities.

## Strategies

Common reasons to use scripts may include (ideas for tests and examples):

- permute test variables to cover many sub-conditions in a test
- automatically adjust load factors to identify the nominal capacity of a system
- Adjust rate of a workload in order to get a specific measurement of system behavior
- React to changes in test or target system state in order to properly sequence a test

## Future Work
It may make sense to provide an eventing interface between the control script sandbox and the core runtime.
This and similar ideas are in active discussion. If this would be useful to you, and you have a clear idea about
how you might want it to work, a feature request would be welcome.
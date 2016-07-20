# Engine Block Scripting

## Motive

The EB runtime is a combination of a scripting sandbox and a workload execution machine. This is not accidental. With this particular arrangement, it should be possible to build sophisticated tests across a variety of scenarios. In particular, logic which can observe and react to the system under test can be powerful. With this approach, it becomes possible to break away from the conventional run-interpret-adjust cycle which is all too often done by human hands.

## Machinery vs Controls

All of the heavy lifting is left to Java and the core EngineBlock runtime. This includes the iterative workloads that are meant to test the target system. This is combined with a control layer which is provided by Nashorn in Java 8. This division of responsibility allows the high-level test logic to be "script" and the low-level activity logic to be "machinery". The net effect is that you have the efficiency of the iterative test loads in conjunction with the expressivity of an open-ended scripting sandbox.

Here is a view of what is below the line, separate form the scripting sandbox, and what is hoisted up into the scripting sandbox.
[ScriptingEngine](diagrams/artandmachinery.png)


## Scripting Environment

The EngineBlock scripting environment is provided by Nashorn, with slight modifications meant to streamline understanding and usage.

The modification are:

- Active Bindings, control variables which, when assigned to, cause an immediate change in the behavior of the runtime. Each of the variables below is pre-wired into each script environment.
  - __sc__ - (read+write) - This is the __Scenario Controller__ object which manages the activity executors in the runtime.
  - __activities.&lt;activity alias&gt;.&lt;paramname&gt;__ - (read+write) - Every parameter which is assigned to in this map will cause a synchronous notification to the execution runtime, including the respective activity (identified by &lt;activity alias&gt;) if it has been implemented to react.
  - __metrics__ - (read-only) - This is the metrics map from the MetricRegistry.getMetrics() call.

Interaction with the EB runtime and the activities therein is made easy by the above variables and objects. When an assignment is made to any of these variables, the changes are propagated to internal listeners. For changes to _threads_, the thread pool responsible for the affected activity adjusts the number of active threads (AKA slots). Other changes are further propagated directly to the thread harness (motor) and to each interested input and action. Assignment to the _type_ and _alias_ activity parameters has no special effect.

You can make use of more extensive Java or Javascript libraries as needed, mixing then with the runtime controls provided above.

## Control Flow

When a script is run, it has absolute control over the scenario runtime while it is active. Once the script reaches its end, however, it will only exit if all activities have completed. If you want to explicitly stop a script, you must stop all activities.

## Strategies

You can use EngineBlock in the classic form with _activity &lt;param&gt;=&lt;value&gt; ..._ command line syntax. There are reasons, however, that you will sometimes want customize and modify your scripts directly, such as:

- permute test variables to cover many sub-conditions in a test
- automatically adjust load factors to identify the nominal capacity of a system
- Adjust rate of a workload in order to get a specific measurement of system behavior
- React to changes in test or target system state in order to properly sequence a test

## Script Input & Output

Internal buffers are kept for _stdin_, _stdout_, and _stderr_.

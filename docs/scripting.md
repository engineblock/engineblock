# Engine Block Scripting

## Motive

The EB runtime is a combination of a scripting sandbox and a workload execution machine. This is not accidental.
With this particular arrangement, it should be possible to build sophisticated tests across a variety of scenarios. 
In particular, logic which can observe and react to the system under test can be powerful. It can make it possible
to break away from the conventional run-interpret-adjust cycle which is all too often done by human hands. The ability
to get a useful set of answers from nontrivial systems with reasonable effort requires a more sophisticated approach.

## Script Environment

The TestScript environment is the standard Nashorn Javascript environment, with slight modifications meant to streamline
understanding and usage. The modification are:

- Active Variables, control variables which, when assigned to, cause an immediate change in the EB runtime. Each of the variables below is pre-wired into each script environment.
- - activities._&lt;alias&gt;.&lt;paramname&gt;_ - (read+write) - Every parameter which is assigned to in this map will cause a synchronous notification to the execution runtime, including the respective activity (identified by &lt;alias&gt;) if it has been implemented to react.
- - metrics._&lt;alias&gt;.&lt;metric&gt;_ - (read-only) - Every value available here is a metrics object from the internal instrumentation.
- - _sc_ - (read+write) - This is the __Scenario Controller__ object which manages the activity executors in the runtime.

Interaction with the EB runtime and the activities therein is made easy by the above variables and objects. When an assignment is made to any of these variables, the changes are propagated to internal listeners. For changes to _threads_, the thread pool responsible for the affected activity adjusts the number of active threads (AKA slots). Other changes are further propagated directly to the thread harness (motor) and to each interested input and action.

You can make use of more extensive Java or Javascript libraries as needed, mixing then with the runtime controls provided above.

## Control Flow

When a script is run, it has absolute control over the scenario runtime while it is active. Once the script reaches its end, however, it will only exit if all activities have completed. If you want to explicitly stop a script, you must stop all activities.

## Strategies

You can use EB in the classic form with _--activity=..._ command line syntax. There are reasons, however, that you will sometimes want customize and modify your scripts directly, such as:

- permute test variables to cover many sub-conditions in a test
- automatically adjust load factors to identify the nominal capacity of a system
- Adjust rate of a workload in order to get a specific measurement of system behavior
- React to changes in test or target system state in order to properly sequence a test

## Script Input & Output

Internal buffers are kept for _stdin_, _stdout_, and _stderr_. Logging for these buffers is separately controllable via the [logging settings](logging.md).

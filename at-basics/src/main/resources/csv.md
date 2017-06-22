# csv activity type

This is an activity type which allows for the generation of data
into a csv file from Powertools EngineBlock.

## Example activity definitions

Run a csv activity named 'csv-test', with definitions from activities/csv-test.yaml
~~~
... type=csv yaml=csv-test
~~~

Only run statement groups which match a tag regex
~~~
... type=csv yaml=csv-test tags=group:'ddl.*'
~~~

Run the matching 'dml' statements, with 100 cycles, from [1000..1100)
~~~
... type=csv yaml=csv-test tags=group:'dml.*' cycles=1000..11000 filename=test.csv
~~~
This last example shows that the cycle range is [inclusive..exclusive),
to allow for stacking test intervals. This is standard across all
activity types.

## csv ActivityType Parameters

- **filename** - this is the name of the output file 
    (defaults to "stdout", which actually writes to stdout, not the filesystem)
- **yaml** - The file which holds the schema and statement defs. 
    (no default, required)
- **cycles** - standard, however the activity type will default 
    this to however many statements are included in the current 
    activity, after tag filtering, etc.
- **alias** - this is a standard engineblock parameter

## Error Handling
#### Error Handlers

When an error occurs, you can control how it is handled. 

This is the error handler stack:

- **stop** - causes the exception to be thrown to the runtime, forcing a shutdown.
- **warn** - log a warning in the log, with details about the error and associated statement.
- **retry** - Retry the operation if the number of retries hasn't been 
    used up.
- **count** - keep a count in metrics for the exception, under the name 
    exceptions.classname, using the simple class name, of course.
- **ignore** - do nothing, do not even retry or count

They are ordered from the most extreme to the most oblivious starting
at the top.  With the exception of the **stop** handler, the rest of 
them will be applied to an error all the way to the bottom. One way 
to choose the right handler is to say "How serious is this to the test
run or the results of the test if it happens?" In general, it is best 
to be more conservative and choose a more aggressive setting unless you 
are specifically wanting to measure how often a given error happens, 
for example.

#### Error Types

The errors that can be detected are sorted into three categories:
~~~
DOCS TBD FOR THIS SECTION

- **unapplied** - This was a LWT that did not get applied. All operations
    are checked, and a ChangeUnapplied exception is thrown.
    (This is a local exception to make error handling consistent)
    This is a separate category from retryable, because you have to
    have reactive logic to properly submit a valid request when it occurs.
~~~
- **retryable** - NoHostAvailable, Overloaded, WriteTimeout, and 
    ReadTimeout exceptions. These are all exceptions that might
    succeed if tried again with the same payload.
- **realerrors** -  ReadFailure, WriteFailure, SyntaxError, InvalidQuery.
    These represent errors that are likely a persistent issue, and
    will not likely succeed if tried again.

To set the error handling behavior, simply pair these categories up with 
an entry point in the error handler stack. Here is an example, showing
also the defaults that are used if you do not specify otherwise:

    retryable=retry realerror=stop 

## Generic Parameters

*provided by the runtime*
- **targetrate** - The target rate in ops/s
- **linkinput** - if the name of another activity is specified, this activity
    will only go as fast as that one.
- **tags** - optional filter for matching tags in yaml sections (detailed help
    link needed)
- **threads** - the number of client threads driving this activity

## Metrics
- \<alias\>.cycles - (provided by core input) A timer around the whole cycle
- \<alias\>.bind - A timer which tracks the performance of the statement
    binding logic, including the generation of data immediately prior
- \<alias\>.execute - A timer which tracks the performance of op submission
    only. This is the async execution call, broken out as a separate step.
- \<alias\>.result - A timer which tracks the performance of an op result only.
    This is the async get on the future, broken out as a separate step.
- \<alias\>.tries - A histogram of how many tries were required to get a
    completed operation

## YAML Format

The YAML file for a file activity has one or more logical yaml documents,
each separted by tree dashes: --- the standard yaml document separator. Each
yaml document may contain a tags section for the purpose of including or 
excluding statements for a given activity: 

~~~ (optional)
tags:
  tagname: value
  ...
~~~
If no tags are provided in a document section, then it will be matched by 
all possible tag filters. Conversely, if no tag filter is applied in 
the activity definition, all tagged documents will match.

Statements can be specified at the top level or within named blocks. When
you have simple needs to just put a few statements into the yaml, the top-level
style will suffice:

~~~
name: statement-top-level-example
statements:
- statement 1
- statement 2
~~~

If you need to represent multiple blocks of statements in the same activity,
you might want to group them into blocks:
~~~
blocks:
- name: statement-block-1
  statements:
  - statement 1
  - statement 2
~~~  

At any level that you can specify statements, you can also specify data bindings:

~~~
statements:
- statement 1
- statement 2
bindings:
 bindto1: foo
 bindto2: bar

blocks:
- name: statement-block-1
  statements:
  - statement 1
  bindings:
    bindto1: foo
~~~

Data bindings specify how values are generated to plug into each operation. More
details on data bindings are available in the activity usage guide.

### Parameter Templating

Double angle brackets may be used to drop parameters into the YAML 
arbitrarily. When the YAML file is loaded, and only then, these parameters
are interpolated from activity parameters like those above. This allows you
to create activity templates that can be customized simply by providing
additional parameters to the activity. There are two forms, 
\<\<some_var_name:default_value\>\> and \<\<some_var_name\>\>. The first
form contains a default value. In any case, if one of these parameters is
encountered and a qualifying value is not found, an error will be thrown.

### YAML Location

The YAML file referenced in the yaml= parameter will be searched for in the following places, in this order:
1. A URL, if it starts with 'http:' or 'https:'
2. The local filesystem, if it exists there
3. The internal classpath and assets in the eb jar.

The '.yaml' suffix is not required in the yaml= parameter, however it is
required on the actual file. As well, the logical search path "activities/"
will be used if necessary to locate the file, both on the filesystem and in
the classpath.

This is a basic example below that can be copied as a starting template.

## YAML Example
    ---
    tags:
     type: testtag
     kind: somekind
     oevure: bananas
    name: outerblock
    statements:
      - foo
      - bar
      - customer
    bindings:
      bar: NumberNameToString()
      foo: NumberNameToString()
      customer: NumberNameToString()

This will output a csv file with 3 columns.

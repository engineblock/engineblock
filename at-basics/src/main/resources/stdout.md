# stdout activity type

This is an activity type which allows for the generation of data
into to stdout or a file.

## Example activity definitions

Run a stdout activity named 'stdout-test', with definitions from activities/stdout-test.yaml
~~~
... type=stdout yaml=stdout-test
~~~

Only run statement groups which match a tag regex
~~~
... type=stdout yaml=stdout-test tags=group:'ddl.*'
~~~

Run the matching 'dml' statements, with 100 cycles, from [1000..1100)
~~~
... type=stdout yaml=stdout-test tags=group:'dml.*' cycles=1000..11000 filename=test.csv
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

## Configuration

This activity type uses the uniform yaml configuration format.
For more details on this format, please refer to the help topic *yaml*.

## Configuration Parameters

There are no configuration-level parameters.

## Statement Format

The

The statement template is optional for this activity. If it is not provided,
then the binding names (if provided) are used to create a default CSV-style
template.
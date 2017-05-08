#at-json

at-json is an activity designed to convert engineblock output into a JSON file

#Parameters

#####yaml - Defines a valid activity definition pulled from a supplied file path

#####alias - Defines an activities name (alias)

#####type - Defines an activities type (at-json)

#####cycles - Defines the number of cycles an activity should be executed

#####threads - Defines the number of threads an activity will run in parallel

#Usage

Initializing a basic at-json activity using the test.yaml file for the supplied yaml parameter

```
type=at-json alias=test yaml=test.yaml
```

Initializing a basic at-json activity using the test.yaml file and running 10 cycles

```
type=at-json alias=test yaml=test.yaml cycles=10
```

Initializing a basic at-json activity using the test.yaml file, 50 cycles and 5 threads

```
type=at-json alias=test yaml=test.yaml cycles=50 threads=5
```
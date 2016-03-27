# Developing with EM

## Requirements

- Java 8
- Maven

## Contributing

This project is eager to have contributors. To that end, pull requests which are in the spirit of the project will be welcome. When pull requests are not directly accepted, kind and specific explanation of why will be provided. If you want to contribute, and are not sure about whether your improvements would be accepted, simply file an issue and describe what you are interested in doing before coding too much.

## Building new Activity types

This is the short-form recipe for building a new driver for EM.

1. Add em to your maven project. Until EM is in a public Maven repo, you can simply build it and install it to your local maven repo.


    git clone http://github.com/jshook/em.git
    pushd em
    mvn install

2. Implement the ActivityType interface.
3. Add the ___ dependency to your project.
4. Annotate your ActivityType implementation with..


    @AutoService(ActivityType.class)
    public class MyNewActivityType implements ActivityType {
    ...

5. Build your artifact.

## Using ActivityTypes

There are a couple ways you can use your new ActivityTypes with the EM runtime. You can mix and match these as needed.

#### Embedding EM core

This is as simple as wrapping the core Maven build of EM as an artifact with the correct Maven Mojo ...

    <build>
    ...
    </build>


#### Loading ActivityType jars at Runtime

By default, EM will add jars in the `lib/` directory to it's class path. If you simply drop any ActivityType artifacts you've built into that directory, the driver types will be discovered.



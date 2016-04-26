# Developing with Engine Block

## Requirements

- Java 8
- Maven

## Building new Activity types

This is the short-form recipe for building a new driver for EM.

1. Add EB to your maven project. Until EB is in a public Maven repo, you can simply build it and install it to your local maven repo.


    git clone http://github.com/jshook/eb.git
    pushd eb
    mvn install

2. Implement the ActivityType interface.
3. Add the ___ dependency to your project.
4. Annotate your ActivityType implementation with..


    @AutoService(ActivityType.class)
    public class MyNewActivityType implements ActivityType {
    ...

5. Build your artifact.

## Using ActivityTypes

There are a couple ways you can use your new ActivityTypes with the EB runtime. You can mix and match these as needed.

#### Embedding Engine Block with Maven

This is as simple as wrapping the core Maven build of EB as an artifact with the correct Maven Mojo ...

    <build>
    ...
    </build>


#### Loading ActivityType jars at Runtime

By default, EB will add jars in the `lib/` directory to it's class path. If you simply drop any ActivityType artifacts you've built into that directory, the driver types will be discovered.



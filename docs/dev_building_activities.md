# Developing with Engine Block

## Requirements

- Java 8
- Maven

## Get EngineBlock

1a. Add EngineBlock to your project via Maven, if you simply want to use a release version. This is sufficient for building ActivityTypes.

~~~
<dependency>
  <groupId>io.engineblock</groupId>
  <artifactId>engineblock</artifactId>
  <version>1.0.3</version>
  <type>pom</type>
</dependency>
~~~

1b. Download and locally build EngineBlock. Do this if you want contribute or otherwise experiment with the EngineBlock code base.

~~~
git clone http://github.com/engineblock/engineblock.git
pushd engineblock
mvn test
~~~

## Building new Activity types

This is the short-form recipe for building a new driver for EngineBlock.

1. Get EngineBlock (above)
2. Implement the ActivityType interface.
3. Implement ActionDispenserProvider in the same class.
4. Use the [Annotated Diag ActivityType] as a reference point as needed.
5. File Issues against the [EngineBlock Project](http://github.com/engineblock/engineblock/issues) for any doc or API enhancements that you need.
6. Add your new ActivityType implementation to the EngineBlock classpath.

## Using ActivityTypes

There are a couple ways you can use your new ActivityTypes with the EB runtime. You can mix and match these as needed.



# Developing with Engine Block

## Requirements

- Java 8
- Maven


## Building new Activity Types

1. Add the engineblock API to your project via Maven:

~~~
<dependency>
  <groupId>io.engineblock</groupId>
  <artifactId>eb-api</artifactId>
  <version>1.0.17</version>
  <type>pom</type>
</dependency>
~~~

2. Implement the ActivityType interface. Use the [Annotated Diag ActivityType] as a reference point as needed.
3. Add your new ActivityType implementation to the EngineBlock classpath.
4. File Issues against the [EngineBlock Project](http://github.com/engineblock/engineblock/issues) for any doc or API enhancements that you need.

## Working directly on engineblock

You can download and locally build EngineBlock. Do this if you want contribute or otherwise experiment with the EngineBlock code base.

1. Get the source:
~~~
git clone http://github.com/engineblock/engineblock.git
~~~

2. Build and install locally:
~~~
pushd engineblock # assumes bash
mvn clean install
~~~

This will install the engineblock artifacts to your local _~/.m2/repository_.


## Using ActivityTypes

There are a couple ways you can use your new ActivityTypes with the EB runtime. You can mix and match these as needed. The most common way to integrate your ActivityTypes with the engineblock core is with Maven, but the details on thi will vary by environment.



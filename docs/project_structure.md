# EM Project Structure

EM is packaged as a [Maven Reactor](https://maven.apache.org/guides/mini/guide-multiple-modules.html) project.

There is no parent project for each of the modules. Dependencies between the modules is explicitly defined in the pom, or there is no dependency. Each module will produce its own artifacts. The project has a dependency structure that is a strict directed graph.

[Project Structure](diagrams/project_structure.png)
## em-runtime

A runtime artifact is created that ties artifacts from all other modules into a single artifact. It is the apex consumer of the other modules, and as-such provides the main artifact for the whole project: __em.jar__.

The full maven coordinates for em-runtime are:
~~~
  <dependency>
    <groupId>com.metawiring</groupId>
    <artifactId>em-runtime</artifactId>
    <version>LATEST</version>
  </dependency>
~~~

The coordinates above are useful for embedding into enhanced distributions of EM when you want to provide your own drivers into a single artifact.

## em-core

The core EM module provides the core machinery needed to execute activities.

## em-api

The API defines the interfaces that must be implemented in order to realize a new ActivityType. The core runtime depends on the _em-api_ module as well, as it is is a consumer of these services. It acts as a protocol bridge between modules wanting to provide ActivityTypes and modules wanting to use them.

When you want to implement your own ActivityType, you'll need the full maven coordinates:
~~~
  <dependency>
    <groupId>com.metawiring</groupId>
    <artifactId>em-core</artifactId>
    <version>LATEST</version>
  </dependency>
~~~

## emd-diag

_Diag_ is an example implementation of an ActivityType. It provides a useful dummy driver for experimentation. The prefix __emd-__ is the naming convention used for artifacts or jars which implement the ActivityType interface. Mnemonic: "EM Driver".

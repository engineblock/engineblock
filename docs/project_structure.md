# EM Project Structure

EM is packaged as a [Maven Reactor](https://maven.apache.org/guides/mini/guide-multiple-modules.html) project.
There is no parent project for each of the modules. Dependencies between the modules is explicit. Each module in the overall project will produce its own artifacts. Further, a bundle artifact is created that ties the whole runtime together into an executable jar.

1. The em-api module holds all interfaces that are required for inter-module linking. Modules that use or provide ActivityTypes depend on this module.
2. The em-core module holds the core runtime.
3. The em-drivers module contains some basic ActivityType implementations. It can be used as an example for those wishing to emulate the maven process for building their own driver.
3. The em-datagen module contains basic data generators and code examples for how to implement them.
4. The em-bundle module contains the Maven mojo for building the bundle jar: em.jar.

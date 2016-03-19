# TestClient Quick Start

TestClient is released as an executable jar.

To use a specific release, simply download it and run it like this:
    
    curl -O https://github.com/jshook/testclient/releases/download/1.0.2-snapshot/testclient.jar
    java -jar testclient.jar
        
The latest release can always be run with this simple script: [run-testclient](https://raw.githubusercontent.com/jshook/testclient/archwork/run-testclient). You can use the commands below to get it.

    curl -O https://raw.githubusercontent.com/jshook/testclient/archwork/run-testclient
    chmod u+x run-testclient
    
This script may be used in place of

    java -jar testclient.jar arg0 ...
    
It will simply download testclient.jar from the latest release if needed, and then invoke it with the supplied command line arguments.

## Select an activity type to run

    ./run-testclient --list-activity-types

This should provide a list of the activity types that are known to the client.

You can get further documentation about an activity:

    ./run-testclient --activity-help diag

You can run an instance of an activity from the available types:

    ./run-testclient --activity alias=test1;type=diag;threads=10;interval=200;

You can run multiple activities:

    ./run-testclient \
    --activity alias=test1;type=diag;threads=10;interval=200; \
    --activity alias=test2;type=diag;threads=1;interval=10;
    
## The Command Line

There are many other command line options available. For more advanced usage, 
consult the [Command Line Reference](command_line.md), or even the full
[Usage Guide](usage_guide.md)
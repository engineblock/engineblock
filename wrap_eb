#!/bin/bash

( cat <<"EOSTUB"
#!/bin/bash
MYSELF=`which "$0" 2>/dev/null`
[ $? -gt 0 -a -f "$0" ] && MYSELF="./$0"
java=java
if test -n "$JAVA_HOME"; then
    java="$JAVA_HOME/bin/java"
fi
#printf "java='%s' java_args='%s' MYSELF='%s'\n" "${java}" "${java_args}" "${MYSELF}"
exec "$java" $java_args -jar $MYSELF "$@"
exit 1 
EOSTUB
) >bin/eb

cat eb-cli/target/eb-cli.jar >>bin/eb
chmod +x bin/eb


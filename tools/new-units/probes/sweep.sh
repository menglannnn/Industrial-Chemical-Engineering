#!/bin/bash
# sweep.sh <name> <kinds...>: the integration sweep of the kinds into build/tmp/units/int/sweep-<name>.txt
export MSYS_NO_PATHCONV=1
cd "$(dirname "$0")/../../../../.."
G="C:/Users/wormz/.gradle/caches/modules-2/files-2.1"
JUNIT="$G/org.junit.jupiter/junit-jupiter-api/5.11.4/308315b28e667db4091b2ba1f7aa220d1ddadb97/junit-jupiter-api-5.11.4.jar;$G/org.junit.platform/junit-platform-commons/1.11.4/8898eea3ed0da2641548d602c3e308804f166303/junit-platform-commons-1.11.4.jar;$G/org.opentest4j/opentest4j/1.3.0/152ea56b3a72f655d4fd677fc0ef2596c3dd5e6e/opentest4j-1.3.0.jar;$G/org.apiguardian/apiguardian-api/1.1.2/a231e0d844d2721b0fa1b238006d15c6ded6842a/apiguardian-api-1.1.2.jar"
name=$1; shift
java -Xshare:off -Xmx2g -cp "build/tmp/units/int/probe-out2;build/classes/java/main;build/classes/java/test;build/resources/main;$JUNIT" com.wormzjl.createcheme.science.equipment.Sweep "$@" > build/tmp/units/int/sweep-$name.txt 2>&1
echo "exit $?" >> build/tmp/units/int/sweep-$name.txt

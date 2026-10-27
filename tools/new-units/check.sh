#!/bin/bash
# check.sh <lane> <KIND[,KIND]> <source files...>
# A lane's gate without Gradle (several lanes share one worktree): compiles the lane's own sources (its designs) into
# build/tmp/units/<lane>/out against the last Gradle build, then runs the equipment tests and the determinism source
# gate with that directory first on the class path, and prints the lane's kinds' counts. Exit code 1 on a failure.
# Run from the worktree root after `./gradlew compileJava compileTestJava` has built the scaffold once.
set -u
export MSYS_NO_PATHCONV=1  # Git Bash would rewrite the ;-separated class path
cd "$(dirname "$0")/../.."
lane=$1 kinds=$2
shift 2
G="C:/Users/wormz/.gradle/caches/modules-2/files-2.1"
JUNIT="$G/org.junit.jupiter/junit-jupiter-api/5.11.4/308315b28e667db4091b2ba1f7aa220d1ddadb97/junit-jupiter-api-5.11.4.jar;$G/org.junit.platform/junit-platform-commons/1.11.4/8898eea3ed0da2641548d602c3e308804f166303/junit-platform-commons-1.11.4.jar;$G/org.opentest4j/opentest4j/1.3.0/152ea56b3a72f655d4fd677fc0ef2596c3dd5e6e/opentest4j-1.3.0.jar;$G/org.apiguardian/apiguardian-api/1.1.2/a231e0d844d2721b0fa1b238006d15c6ded6842a/apiguardian-api-1.1.2.jar"
OUT="build/tmp/units/$lane/out"
mkdir -p "$OUT"
javac -encoding UTF-8 -nowarn -cp "build/classes/java/main;build/classes/java/test;$JUNIT" -d "$OUT" "$@" tools/new-units/UnitCounts.java tools/new-units/UnitRender.java tools/new-units/RenderAll.java tools/new-units/TestRunner.java || exit 1
CP="$OUT;build/classes/java/main;build/classes/java/test;build/resources/main;$JUNIT"
P=com.wormzjl.createcheme.science
java -Xshare:off -Xmx1536m -cp "$CP" TestRunner $P.equipment.ProcessUnitTest $P.equipment.ShapeRasterizerTest $P.equipment.LineGeometryTest $P.fluid.DeterministicMathSourceTest
status=$?
java -Xshare:off -Xmx1536m -cp "$CP" $P.equipment.UnitCounts ${kinds//,/ }
exit $status

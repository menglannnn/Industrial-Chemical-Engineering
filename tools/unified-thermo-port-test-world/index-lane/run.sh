#!/usr/bin/env bash
# run.sh <label> <selector> [-Dkey=value ...]: runs JUnit selectors ("pkg.Class" or "pkg.Class#method", M3Run) on the
# classes compile.sh built in /tmp/testworld-<label>/, with that export's resources, in one JVM (-Xmx6g). Refuses while a
# Minecraft dev client runs. The rig: run.sh <label> com.wormzjl.icheme.runtime.fluid.TestWorldRigTest
#   -DtestWorld.rows=<rows dir> -DtestWorld.presets=<mixturepreset dir> [-DtestWorld.only=R3,R11]
set -u
LABEL=${1:?label}; SELECTOR=${2:?selector}; shift 2
WT=${WT:-D:/Minecraft/Modding/1.21/CreateChemE/.claude/worktrees/coolprop-multiphase-thermo-37f6b0}
HERE=$(cd "$(dirname "$0")" && pwd)
JDK="C:/Program Files/Java/jdk-21.0.11/bin"
G=C:/Users/wormz/.gradle/caches/modules-2/files-2.1
JUNIT="$G/org.junit.jupiter/junit-jupiter-api/5.11.4/308315b28e667db4091b2ba1f7aa220d1ddadb97/junit-jupiter-api-5.11.4.jar;$G/org.junit.jupiter/junit-jupiter-engine/5.11.4/dc10ec209623986a68ea07f67cdc7d2a65a60355/junit-jupiter-engine-5.11.4.jar;$G/org.junit.jupiter/junit-jupiter-params/5.11.4/e4c86fbe2a39c60c6b87260ef7f7e7c1a1906481/junit-jupiter-params-5.11.4.jar;$G/org.junit.platform/junit-platform-commons/1.11.4/8898eea3ed0da2641548d602c3e308804f166303/junit-platform-commons-1.11.4.jar;$G/org.junit.platform/junit-platform-engine/1.11.4/21f61b123ad6ac8f7e73971bff3a096c8d8e1cd0/junit-platform-engine-1.11.4.jar;$G/org.junit.platform/junit-platform-launcher/1.11.4/3d83c201899d8c5e74e1a5d628eab900342a0e48/junit-platform-launcher-1.11.4.jar;$G/org.opentest4j/opentest4j/1.3.0/152ea56b3a72f655d4fd677fc0ef2596c3dd5e6e/opentest4j-1.3.0.jar;$G/org.apiguardian/apiguardian-api/1.1.2/a231e0d844d2721b0fa1b238006d15c6ded6842a/apiguardian-api-1.1.2.jar"
live=$(powershell.exe -NoProfile -Command "@(Get-CimInstance Win32_Process -Filter \"Name='java.exe' OR Name='javaw.exe'\" | Where-Object { \$_.CommandLine -match 'fml.modFolders' }).Count" | tr -d '\r')
if [ "${live:-0}" != "0" ]; then echo "run.sh: a Minecraft dev client is running ($live); wait until it has exited"; exit 3; fi
X=/tmp/testworld-$LABEL; XW=$(cygpath -m "$X")
[ -d "$X/test" ] || { echo "run.sh: compile $LABEL first"; exit 2; }
mkdir -p /tmp/testworld-launcher
[ -e /tmp/testworld-launcher/M3Run.class ] || "$JDK/javac" -nowarn -d "$(cygpath -m /tmp/testworld-launcher)" -cp "$JUNIT" "$(cygpath -m "$HERE/M3Run.java")"
libs=$(sed -e 's/^-cp "//' -e 's/"$//' "$X/main-cp.txt")
printf -- '-cp "%s"\n' "$XW/test;$XW/src/test/resources;$XW/main;$XW/src/main/resources;$libs;$JUNIT;$(cygpath -m /tmp/testworld-launcher)" > "$X/run-cp.txt"
echo "run.sh label=$LABEL commit=$(cat "$X/COMMIT") selector=$SELECTOR $*"
"$JDK/java" -Xshare:off -Xmx6g "$@" @"$XW/run-cp.txt" M3Run "$SELECTOR"

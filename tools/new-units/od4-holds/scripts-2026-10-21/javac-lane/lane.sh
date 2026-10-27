#!/usr/bin/env bash
# D40 lane (copied from ../simplifications/lane.sh, exports under D:/tmp_od4r3/*), Gradle-free (adapted from
# tools/unified-thermo-port-forms/scripts/export.sh, index-compile.sh, index-run.sh):
#   bash lane.sh export  <name> <tree-ish> [patch...]   git archive of the worktree's tree-ish (src/main, src/test and
#                                                      src/fluidGameTest) into /tmp/od4-<name>, then the patches applied
#                                                      there (never touches the worktree)
#   bash lane.sh compile <name> [test files...]        javac of every main source, then the named test sources (paths
#                                                      relative to src/test/java, or absolute), their dependencies found
#                                                      through -sourcepath; "all" compiles every test source and every
#                                                      fluidGameTest source (against main, as its source set does)
#   bash lane.sh run     <name> <selector...>          JUnit selectors "pkg.Class" or "pkg.Class#method" with M3Run, one
#                                                      JVM at -Xmx6g; JAVA_OPTS passes system properties
# The classpath is the worktree's build/moddev files (read only).
# Prints "lane compile <name> main=<rc> test=<rc> gametest=<rc>".
set -u
WT=${WT:-D:/Minecraft/Modding/1.21/CreateChemE/.claude/worktrees/custom-equipment-rendering-805e7d}
JDK="C:/Program Files/Java/jdk-21.0.11/bin"
G=C:/Users/wormz/.gradle/caches/modules-2/files-2.1
JA="$G/org.junit.jupiter/junit-jupiter-api/5.11.4/308315b28e667db4091b2ba1f7aa220d1ddadb97/junit-jupiter-api-5.11.4.jar;$G/org.junit.jupiter/junit-jupiter-params/5.11.4/e4c86fbe2a39c60c6b87260ef7f7e7c1a1906481/junit-jupiter-params-5.11.4.jar;$G/org.junit.platform/junit-platform-commons/1.11.4/8898eea3ed0da2641548d602c3e308804f166303/junit-platform-commons-1.11.4.jar;$G/org.opentest4j/opentest4j/1.3.0/152ea56b3a72f655d4fd677fc0ef2596c3dd5e6e/opentest4j-1.3.0.jar;$G/org.apiguardian/apiguardian-api/1.1.2/a231e0d844d2721b0fa1b238006d15c6ded6842a/apiguardian-api-1.1.2.jar"
JE="$G/org.junit.jupiter/junit-jupiter-engine/5.11.4/dc10ec209623986a68ea07f67cdc7d2a65a60355/junit-jupiter-engine-5.11.4.jar;$G/org.junit.platform/junit-platform-engine/1.11.4/21f61b123ad6ac8f7e73971bff3a096c8d8e1cd0/junit-platform-engine-1.11.4.jar;$G/org.junit.platform/junit-platform-launcher/1.11.4/3d83c201899d8c5e74e1a5d628eab900342a0e48/junit-platform-launcher-1.11.4.jar"
mode=${1:?usage: lane.sh export|compile|run <name> ...}; name=${2:?name}; shift 2
D=/d/tmp_285/x/$name; X=$(cygpath -m "$D")
case "$mode" in
  export)
    tree=${1:?tree-ish}; shift
    rm -rf "$D"; mkdir -p "$D"
    git -C "$WT" archive "$tree" src/main/java src/test/java src/main/resources src/test/resources src/fluidGameTest | tar -x -C "$D"
    for p in "$@"; do
      for f in $(grep '^+++ b/' "$p" | sed 's|^+++ b/||'); do if [ -f "$D/$f" ]; then sed -i 's/\r$//' "$D/$f"; fi; done
      (cd "$D" && git apply --verbose "$(cygpath -m "$p")") || { echo "patch $p failed"; exit 3; }
    done
    echo "lane export $tree -> $D${*:+ with $*}" ;;
  compile)
    [ -d "$D/src/main/java" ] || { echo "no export at $D"; exit 2; }
    rm -rf "$D/main" "$D/test" "$D/gt"; mkdir -p "$D/main" "$D/test" "$D/gt"
    libs="$WT/build/moddev/artifacts/neoforge-21.1.219-merged.jar;C:/Users/wormz/.gradle/caches/modules-2/files-2.1/net.createmod.ponder/ponder-neoforge/1.0.82+mc1.21.1/b10a04f40b9d6d33c346fb38df33f10f7e51b713/ponder-neoforge-1.0.82+mc1.21.1.jar;C:/Users/wormz/.gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.21.1/6.0.10-280/92471e8fc5ed4e3c2279001d77ebcec6fd38bcab/create-1.21.1-6.0.10-280-slim.jar;C:/Users/wormz/.gradle/caches/modules-2/files-2.1/dev.engine-room.flywheel/flywheel-neoforge-1.21.1/1.0.6/a943872ad0bdd4458d818d8d7dc123fb250eb744/flywheel-neoforge-1.21.1-1.0.6.jar;C:/Users/wormz/.gradle/caches/modules-2/files-2.1/dev.engine-room.flywheel/flywheel-neoforge-api-1.21.1/1.0.6/78de23fcd1796061b2ffe9b8a98d38c2a0e56b3f/flywheel-neoforge-api-1.21.1-1.0.6.jar;$G/org.jetbrains/annotations/24.1.0/7af6a669488450c4a07c2c3254e2151df42d7d04/annotations-24.1.0.jar;$(cat "$WT/build/moddev/fluidGameTestServerLegacyClasspath.txt" "$WT/build/moddev/mcpClientLegacyClasspath.txt" | tr -d '\r' | tr '\n' ';')"
    libs=$(printf '%s' "$libs" | tr '\\' '/')
    printf -- '-cp "%s"\n' "$libs" > "$D/main-cp.txt"
    printf -- '-cp "%s;%s;%s"\n' "$X/main" "$libs" "$JA" > "$D/test-cp.txt"
    CREATE="$G/com.simibubi.create/create-1.21.1/6.0.10-280/92471e8fc5ed4e3c2279001d77ebcec6fd38bcab/create-1.21.1-6.0.10-280-slim.jar"
    printf -- '-cp "%s;%s;%s"\n' "$X/main" "$libs" "$CREATE" > "$D/gt-cp.txt"
    (cd "$D" && find src/main/java -name '*.java') | sed "s|^|$X/|" > "$D/main.txt"
    : > "$D/test.txt"; all=0
    for t in "$@"; do
      if [ "$t" = all ]; then all=1; (cd "$D" && find src/test/java -name '*.java') | sed "s|^|$X/|" >> "$D/test.txt"
      elif [ "${t#/}" != "$t" ] || [ "${t#?:}" != "$t" ]; then echo "$t" >> "$D/test.txt"
      else echo "$X/src/test/java/$t" >> "$D/test.txt"; fi
    done
    "$JDK/javac" -nowarn -encoding UTF-8 -proc:none -d "$X/main" @"$X/main-cp.txt" @"$X/main.txt" > "$D/main.log" 2>&1; m=$?
    t=skipped
    if [ $m -eq 0 ] && [ -s "$D/test.txt" ]; then
      "$JDK/javac" -nowarn -encoding UTF-8 -proc:none -implicit:class -sourcepath "$X/src/test/java" -d "$X/test" @"$X/test-cp.txt" @"$X/test.txt" > "$D/test.log" 2>&1; t=$?
    fi
    g=skipped
    if [ $m -eq 0 ] && [ $all -eq 1 ] && [ -d "$D/src/fluidGameTest/java" ]; then
      (cd "$D" && find src/fluidGameTest/java -name '*.java') | sed "s|^|$X/|" > "$D/gt.txt"
      "$JDK/javac" -nowarn -encoding UTF-8 -proc:none -d "$X/gt" @"$X/gt-cp.txt" @"$X/gt.txt" > "$D/gt.log" 2>&1; g=$?
    fi
    echo "lane compile $name main=$m test=$t gametest=$g"
    [ $m -eq 0 ] || grep -E "error|\.java:[0-9]+" "$D/main.log" | head -20
    [ "$t" = 0 ] || [ "$t" = skipped ] || grep -E "error|\.java:[0-9]+" "$D/test.log" | head -20
    [ "$g" = 0 ] || [ "$g" = skipped ] || grep -E "error|\.java:[0-9]+" "$D/gt.log" | head -20 ;;
  run)
    [ -f "$D/main-cp.txt" ] || { echo "not compiled: lane.sh compile $name"; exit 2; }
    L=/d/tmp_od4r3/launcher; mkdir -p "$L"
    [ -e "$L/M3Run.class" ] || "$JDK/javac" -nowarn -d "$(cygpath -m "$L")" -cp "$JA;$JE" "D:/Minecraft/Modding/1.21/CreateChemE/tools/unified-thermo-port-m3/scripts/M3Run.java"
    libs=$(sed -e 's/^-cp "//' -e 's/"$//' "$D/main-cp.txt")
    printf -- '-cp "%s"\n' "$X/test;$X/src/test/resources;$X/main;$X/src/main/resources;$libs;$JA;$JE;$(cygpath -m "$L")" > "$D/run-cp.txt"
    "$JDK/java" -Xshare:off -Xmx6g -Duser.language=en -Duser.country=US ${JAVA_OPTS:-} @"$X/run-cp.txt" M3Run "$@" ;;
  *) echo "usage: lane.sh export|compile|run <name> ..."; exit 2 ;;
esac

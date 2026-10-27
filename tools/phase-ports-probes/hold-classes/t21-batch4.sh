#!/usr/bin/env bash
# t21: every E6 seed of ff13 traced with t13's failing-row trace (src/instr-t21-t13-trace-on-1b49f341.patch, applied in the
# working tree by the caller), for t13-classify.js; the patch is reversed at the end. From the worktree root, in a lane.
set -u
H=tools/phase-ports-probes/hold-classes
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.11" JAVA_OPTS=-Xshare:off
./gradlew --no-configuration-cache compileJava compileTestJava --rerun-tasks --console=plain > $H/logs/ff17-compile.gradle.txt 2>&1
bash $H/t13-replay.sh logs/ff17-e6-trace "175,244,273,322,352,397,401,556,788,854,915,937,951,981" "597,660,698,757,853,916"
git apply -R $H/src/instr-t21-t13-trace-on-1b49f341.patch
./gradlew --no-configuration-cache compileJava compileTestJava --rerun-tasks --console=plain > $H/logs/ff17-restored.gradle.txt 2>&1
git status --short src/

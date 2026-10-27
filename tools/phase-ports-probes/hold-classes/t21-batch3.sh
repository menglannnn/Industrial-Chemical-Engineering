#!/usr/bin/env bash
# t21: the two minimized worse worlds (ff15) traced with src/instr-t21-trace.patch on the final code, then replayed with the
# loss row restored (src/scratch-t21-loss-row.patch); both patches are reversed at the end. From the worktree root, in a lane.
set -u
H=tools/phase-ports-probes/hold-classes; T=C:/Users/wormz/.claude/jobs/20526532/tmp
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.11" JAVA_OPTS=-Xshare:off
K951='5@424,6@424,9@424,11@424,12@424,14@424'; K322='0@498,4@498,6@498,8@498,13@498,14@498,16@498,17@539'
fresh(){ ./gradlew --no-configuration-cache compileJava compileTestJava --rerun-tasks --console=plain > "$H/logs/ff16-$1-compile.gradle.txt" 2>&1; }
git apply $H/src/instr-t21-trace.patch && fresh trace
for w in 951 322; do k=K$w; rm -f "$T/ff16-d$w.txt"
  JAVA_TOOL_OPTIONS="-Dt19trace=$T/ff16-d$w.txt" SLICES=120 bash $H/t21-minimize.sh logs/ff16-trace replay d $w "${!k}"
  grep -E "PASS|FAIL|CONV|CYCLE|FAILCAPS|NEWTON" "$T/ff16-d$w.txt" | sed -E 's/ modes=\[[^]]*\]//; s/ closed=\[[^]]*\]//' | tail -400 > "$H/logs/ff16-trace-d$w-passes.txt"
done
git apply -R $H/src/instr-t21-trace.patch
git apply $H/src/scratch-t21-loss-row.patch && fresh lossrow
for w in 951 322; do k=K$w; SLICES=120 bash $H/t21-minimize.sh logs/ff16-lossrow replay d $w "${!k}"; done
git apply -R $H/src/scratch-t21-loss-row.patch
fresh restored
git status --short src/

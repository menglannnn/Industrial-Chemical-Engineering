#!/usr/bin/env bash
# t13 (2026-10-13): replays the q04 E6 seeds alone to their first hold with the t13 trace (src-t13/T13TraceProbeTest.trace,
# src/instr-t13-failing-rows.patch applied), one Gradle invocation per distribution, and compares each first hold with the
# campaign's: SAME (same tick, committed tick and status), CLEAN (the whole seed runs without a hold), or the new first hold.
# EXTRA forwards switches (-Pt13iters=60, -Pt13l1=true, -Pt13l2=true). From the worktree root.
# Usage: t13-replay.sh <logs/NN-tag> [defaults seeds "1,19"|-] [random seeds|-]   (default: the 77 E6 seeds of q04)
set -u
H=tools/phase-ports-probes/hold-classes; p="$H/$1"; mkdir -p "$p"
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.11" JAVA_OPTS=-Xshare:off
E6D=$(grep '^E6 ' $H/logs/q04-campaign-on-defaults-report.txt.classes.txt | sed 's/.*list= *//' | tr -s ' ' ',')
E6R=$(grep '^E6 ' $H/logs/q04-campaign-on-random-report.txt.classes.txt | sed 's/.*list= *//' | tr -s ' ' ',')
for dist in defaults random; do
  seeds=${2:-$E6D}; flag=true; [ $dist = random ] && { seeds=${3:-$E6R}; flag=false; }
  [ "$seeds" = "-" ] && continue
  ./gradlew --no-configuration-cache --init-script $H/t13.init.gradle test --tests '*T13TraceProbeTest.trace' -Pt13seeds=$seeds -PholdFuzzDefaults=$flag \
    -Pt13out="$PWD/$p" ${EXTRA:-} --console=plain > "$p-$dist.gradle.txt" 2>&1
  grep -h "T13_DONE" "$p-$dist.gradle.txt" | sed -E 's/^ *T13_DONE seed=([0-9]+) (.*) SIG (.*)$/\1|\2|\3/' | while IFS='|' read s held sig; do
    camp=$(awk -v S=$s '$1=="SEED"&&$2==S{f=1;next} f&&/^  HELD /{print;exit}' $H/logs/q04-campaign-on-$dist-report.txt | sed -E 's/.*tick=([0-9]+) island=[0-9]+ committed=([0-9]+) class=\S+ status=(.*)/\1 \2 \3/')
    now=$(echo "$held" | sed -E 's/Held\[tick=([0-9]+), island=[0-9]+, committed=([0-9]+), status=(.*)\]/\1 \2 \3/')
    if [ "$held" = "null" ]; then echo "$dist $s CLEAN"; elif [ "$now" = "$camp" ]; then echo "$dist $s SAME"; else echo "$dist $s NEW tick=$(echo $now | cut -d' ' -f1) ${sig:0:90}"; fi
  done
done | tee "$p-summary.txt" | awk '{print $3}' | sort | uniq -c

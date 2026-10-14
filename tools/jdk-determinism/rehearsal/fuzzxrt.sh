#!/usr/bin/env bash
# The placement-sequence fuzz (both distributions, 4 workers) through the Gradle-free fuzz runner of
# claude/fuzz-runner (read-only, $FR), under another runtime: build/fuzz-runner/java.txt of this worktree is pointed at
# the runtime for the run and restored afterwards (the classes, classpath and JVM arguments are the ones build.sh wrote).
#   fuzzxrt.sh <runtime: jdk21|zulu25|jbr25|openj9|graal25> <tag> [seeds=1-200]     reports: logs/fuzz/<tag>-<runtime>-<dist>-report.txt
set -uo pipefail
HERE=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
ROOT=$(cd "$HERE/../../.." && pwd)
FR=${FR:-/d/Minecraft/Modding/1.21/CreateChemE/.claude/worktrees/agent-a0fdb6ef46fa27b13/tools/phase-ports-probes/fuzz-runner}
if [ ! -f "$FR/campaign.sh" ]; then   # that worktree was removed (2026-10): a read-only copy of the branch's runner
  FR="$ROOT/build/winsweep/fr/tools/phase-ports-probes/fuzz-runner"
  [ -f "$FR/campaign.sh" ] || { mkdir -p "$ROOT/build/winsweep/fr" && git -C "$ROOT" archive claude/fuzz-runner tools/phase-ports-probes/fuzz-runner | tar -x -C "$ROOT/build/winsweep/fr"; } || exit 2
fi
rt=$1; tag=$2; seeds=${3:-1-200}
case $rt in
  jdk21) j='C:\Program Files\Java\jdk-21.0.11\bin\java.exe' ;;
  zulu25) j='C:\Program Files\Zulu\zulu-25\bin\java.exe' ;;
  jbr25) j='C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.1\jbr\bin\java.exe' ;;
  openj9) j='D:\Minecraft\Modding\jdks\jdk-21.0.12.15\bin\java.exe' ;;
  graal25) j='D:\Minecraft\Modding\jdks\graalvm-community-25.4.4.1.1+1.1\bin\java.exe' ;;
  *) echo "unknown runtime $rt"; exit 2 ;;
esac
jt="$ROOT/build/fuzz-runner/java.txt"
cp "$jt" "$jt.orig"
printf '%s\n' "$j" > "$jt"
touch -r "$jt.orig" "$jt"   # keep the stamp order: java.txt is not a source, but do not look newer than the stamp
mkdir -p "$HERE/logs/fuzz"
THREADS=4 JVM_OPTS="-XshowSettings:properties" bash "$FR/campaign.sh" "$ROOT" "$HERE/logs/fuzz/$tag-$rt" "$seeds"
cp "$jt.orig" "$jt"; rm -f "$jt.orig"
grep -hE "java\.(runtime|vm)\.(version|vendor)|java\.vendor\.version" "$HERE/logs/fuzz/$tag-$rt-random.java.txt" | head -4

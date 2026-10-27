#!/usr/bin/env bash
# Between-commit proof on the Gradle-free javac lane: exports the worktree's current src/ tree (committed or not),
# compiles everything, runs the named test groups and compares each group's printed output with the baseline run.
#   bash lane-check.sh <name> [group...]        groups: classes-<group>.txt of tools/unified-thermo-port-m5/d40
#                                               (default: all 16 suite groups); BASE (default c0) names the baseline logs
# Logs: /tmp/t35-logs/<name>-<group>.txt, comparisons cmp-<name>-<group>.txt. Prints one line per group:
# the M3RUN summary, then same/moved counts and how many moved tests differ beyond timing and allocation fields.
set -u
WT=${WT:-D:/Minecraft/Modding/1.21/CreateChemE/.claude/worktrees/coolprop-multiphase-thermo-37f6b0}
D=$WT/tools/unified-thermo-port-m5/d40; T=$WT/tools/unified-thermo-consolidation; L=/tmp/t35-logs; mkdir -p "$L"
name=${1:?usage: lane-check.sh <name> [group...]}; shift
BASE=${BASE:-c0}
ALL="science-thermo science-thermo-phase science-thermo-qualification science-thermo-reference science-material science-fluid science-fluid-thermo science-fluid-solver science-fluid-topology science-fluid-transport science-fluid-network runtime runtime-fluid network client-gui-common client-gui-screens-inventory"
groups=${*:-$ALL}
tree=$(bash "$D/tree.sh") || exit 2
bash "$D/lane.sh" export "$name" "$tree" > "$L/$name-export.txt" 2>&1 || { cat "$L/$name-export.txt"; exit 2; }
bash "$D/lane.sh" compile "$name" all > "$L/$name-compile.txt" 2>&1
grep -q "main=0 test=0 gametest=0" "$L/$name-compile.txt" || { cat "$L/$name-compile.txt"; exit 3; }
LOGS=$L bash "$D/run-touching.sh" "$name" $groups > "$L/$name-run.txt" 2>&1
for g in $groups; do
  perl "$T/compare-test-output.pl" "$L/$name-$g.txt" "$L/$BASE-$g.txt" > "$L/cmp-$name-$g.txt" 2>&1
  physics=$(perl "$T/notime2.pl" "$L/cmp-$name-$g.txt" | grep -c '^PHYSICS')
  printf "%-30s %s | %s | physics=%s\n" "$g" "$(grep -E "^$g " "$L/$name-run.txt" | sed 's/^[^ ]* [^ ]* //')" "$(tail -1 "$L/cmp-$name-$g.txt")" "$physics"
  grep -E '^M3RUN (FAILED|ABORTED)' "$L/$name-$g.txt" | grep -v PackagedSparseLuTest | head -5
  perl "$T/notime2.pl" "$L/cmp-$name-$g.txt" | grep '^PHYSICS' | head -5
  grep -E '^(NEW ONLY|BASELINE ONLY)' "$L/cmp-$name-$g.txt" | head -5
done

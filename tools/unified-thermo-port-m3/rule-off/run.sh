#!/usr/bin/env bash
# M3 rule-off proofs: for each gate in rules.txt (or the gates named as arguments), switch its rule off in a copy of the
# main source, compile on the javac lane, run that one PrecipitatedSolidsTest method, and restore the source byte for
# byte. Logs go to ../logs/rule-off-<rule>.txt. Usage: run.sh [gate-test ...]
set -u
WT=D:/Minecraft/Modding/1.21/CreateChemE/.claude/worktrees/coolprop-multiphase-thermo-37f6b0
HERE=$WT/tools/unified-thermo-port-m3/rule-off; LANE=$WT/tools/unified-thermo-port-m3/scripts/javac-lane.sh
SRC=$WT/src/main/java/com/wormzjl/createcheme/science/fluid; LOGS=$WT/tools/unified-thermo-port-m3/logs
mkdir -p "$LOGS" /tmp/m3/ruleoff
grep -v '^#' "$HERE/rules.txt" | while IFS='|' read -r test rule file spec; do
  if [ $# -gt 0 ] && ! printf '%s\n' "$@" | grep -qx "$test"; then continue; fi
  target="$SRC/$file"; cp "$target" /tmp/m3/ruleoff/backup.java
  log="$LOGS/rule-off-$rule.txt"
  { echo "rule off: $rule ($file, $spec)"; cat "$HERE/$spec"; echo; } > "$log"
  if node "$WT/tools/unified-thermo-port-m2/scripts/edit.js" "$target" "$HERE/$spec" >> "$log" 2>&1; then
    (cd "$WT" && git diff --no-color -- "src/main/java/com/wormzjl/createcheme/science/fluid/$file" | tail -n 25) >> "$log" 2>&1
    bash "$LANE" main >> "$log" 2>&1
    bash "$LANE" run "com.wormzjl.createcheme.science.fluid.network.PrecipitatedSolidsTest#$test" >> "$log" 2>&1
  fi
  cp /tmp/m3/ruleoff/backup.java "$target"
  echo "$rule: $(grep -h 'M3RUN SUMMARY' "$log")"; grep -h 'M3RUN   ' "$log" | head -2
done
bash "$LANE" main > /dev/null 2>&1

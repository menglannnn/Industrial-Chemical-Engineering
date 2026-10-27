#!/usr/bin/env bash
# M4 rule-off proofs: for each line of rules.txt (or the rule names given as arguments), the working tree's sources are
# exported to /tmp/m4/ruleoff with the rule switched off in the copy (scripts/lane.sh EDIT=..., the worktree's sources
# are never touched), compiled whole, and that one test is run alone; then the same test on the unedited copy. Logs:
# ../logs/rule-off-<rule>.txt. Usage: run.sh [rule-name ...]
set -u
WT=${WT:-D:/Minecraft/Modding/1.21/CreateChemE/.claude/worktrees/coolprop-multiphase-thermo-37f6b0}
HERE=$WT/tools/unified-thermo-port-m4/rule-off; LANE=$WT/tools/unified-thermo-port-m4/scripts/lane.sh; LOGS=$WT/tools/unified-thermo-port-m4/logs
mkdir -p "$LOGS"
grep -v '^#' "$HERE/rules.txt" | while IFS='|' read -r test rule file spec; do
  if [ $# -gt 0 ] && ! printf '%s\n' "$@" | grep -qx "$rule"; then continue; fi
  log="$LOGS/rule-off-$rule.txt"
  { echo "rule off: $rule ($file, $spec) at $(cd "$WT" && git rev-parse --short HEAD) plus the working tree"; cat "$HERE/$spec"; echo; } > "$log"
  { echo "== rule off"; EDIT="$file|$HERE/$spec" bash "$LANE" compile work ruleoff; bash "$LANE" run ruleoff "$test"
    echo "== rule on"; bash "$LANE" compile work ruleon; bash "$LANE" run ruleon "$test"; } >> "$log" 2>&1
  echo "$rule: $(grep -h 'M4RUN SUMMARY' "$log" | tr '\n' ' ')"; grep -h 'M4RUN   ' "$log" | head -2
done

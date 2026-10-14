#!/usr/bin/env bash
# Classifies a HoldFuzzTest report by the status of each seed's FIRST hold (and its exception, if any), prints one line per
# class (seeds, smallest seed, seed list) and writes <report>.classes.txt. Usage: classify.sh <report.txt>
report="$1"
awk '
function cls(s) {
  if (s ~ /^HELD \(thermo domain\)/) return "C  thermo-domain wait (typed)";
  if (s ~ /^HELD: wall deadline/ || s ~ /^HELD: soft budget/) return "F  budget (wall deadline / soft budget)";
  if (s ~ /Backward-Euler state change #=[0-9][0-9][0-9]/) return "A  state-change lock (F7)";
  if (s ~ /Matrix values must be finite/) return "B  trace underflow (T1)";
  # The primary failure: the text before the rejection map.
  sub(/[;,] reasons=.*/, "", s);
  if (s ~ /No finite Jacobian/) return "E10 no finite Jacobian trial at a first step";
  if (s ~ /Singular matrix: empty or zero row/) return "G  zero row at the interval start";
  if (s ~ /^HELD: Singular Newton Jacobian/) return "D  singular at the interval-start rate pass";
  if (s ~ /^HELD: Newton iteration limit/ || s ~ /^HELD: Newton line search stalled/ || s ~ /^HELD: Phase\/device active-set cycle/) return "E1 interval-start rate pass does not converge";
  if (s ~ /^HELD: Substep refinement exhausted: Singular Newton Jacobian/) return "E2 step solve singular at every step size";
  if (s ~ /partial pressure below the numerical floor/ || s ~ /Water-vapor approximation outside/) return "E3 vapour partial-pressure floor";
  if (s ~ /equation gate/) return "E4 reconstruction equation gate";
  if (s ~ /^HELD: Substep refinement exhausted: Newton line search stalled/) return "E5 step line search stalls";
  if (s ~ /^HELD: Substep refinement exhausted: Newton iteration limit/) return "E6 step Newton iteration limit";
  if (s ~ /active-set cycle/) return "E7 active-set cycle";
  if (s ~ /Interval substep limit/) return "E8 interval attempt limit (not F7)";
  return "E9 other";
}
/^SEED /{seed=$2; first=""; exc=""}
/^  HELD / && first=="" {s=$0; sub(/.*status=/,"",s); first=s; c=cls(s); list[c]=list[c] " " seed; n[c]++}
/^  EXCEPTION / && exc=="" {exc=$0; c="X  exception: " (exc ~ /outside the islands it fenced/ ? "topology event reached a device outside its fenced islands" : substr(exc,13,60)); list[c]=list[c] " " seed; n[c]++}
END {for (c in n) {split(list[c],a," "); printf "%-60s seeds=%4d smallest=%5s list=%s\n", c, n[c], a[1], list[c]}}
' "$report" | sort > "$report.classes.txt"
cat "$report.classes.txt" | cut -c1-200
grep -h "^SUMMARY" "$report"

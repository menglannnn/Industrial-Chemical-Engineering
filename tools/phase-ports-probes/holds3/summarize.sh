#!/usr/bin/env bash
# Summarises the JUnit XML of the last Gradle test run (build/test-results/test/*.xml) into <prefix>-summary.txt,
# <prefix>-junction-lines.txt (the LIQUID_JUNCTION / MIXED_GAS_* lines of the system-out, ms= masked, as
# ../holds/12-junction-lines.txt), <prefix>-junction-diff.txt (against that reference) and <prefix>-test-names.txt.
# Usage (from the worktree root): tools/phase-ports-probes/holds3/summarize.sh tools/phase-ports-probes/<folder>/logs/NN
set -e
prefix="$1"; reference="${2:-tools/phase-ports-probes/holds/12-junction-lines.txt}"
xml=build/test-results/test
classes=$(ls $xml/TEST-*.xml | wc -l)
tests=0; failures=0; errors=0; skipped=0
for f in $xml/TEST-*.xml; do
  line=$(grep -m1 '<testsuite ' "$f")
  t=$(echo "$line" | sed -E 's/.* tests="([0-9]+)".*/\1/'); fa=$(echo "$line" | sed -E 's/.* failures="([0-9]+)".*/\1/')
  e=$(echo "$line" | sed -E 's/.* errors="([0-9]+)".*/\1/'); s=$(echo "$line" | sed -E 's/.* skipped="([0-9]+)".*/\1/')
  tests=$((tests+t)); failures=$((failures+fa)); errors=$((errors+e)); skipped=$((skipped+s))
done
cat $xml/TEST-*.xml | sed -E 's/^.*<!\[CDATA\[//' | grep -E '^(LIQUID_JUNCTION|MIXED_GAS_COST|MIXED_GAS_TRANSIENT)' | tr -d '\r' > "$prefix-junction-lines-raw.txt" || true
mask='s/ ms=[0-9.]+/ ms=*/; s/ allocatedMB=[0-9.]+/ allocatedMB=*/; s/ bytes=[0-9]+/ bytes=*/'
sed -E "$mask" "$prefix-junction-lines-raw.txt" > "$prefix-junction-lines.txt"
{ echo "classes=$classes tests=$tests failures=$failures errors=$errors skipped=$skipped"; echo "junction lines $(wc -l < "$prefix-junction-lines.txt")";
  grep -h '^MIXED_GAS_COST' "$prefix-junction-lines-raw.txt" || true
  echo "failing:"; grep -l '<failure' $xml/TEST-*.xml | sed -E 's/.*TEST-(.*)\.xml/  \1/' || true; } > "$prefix-summary.txt"
sed -E "$mask" "$reference" | diff "$prefix-junction-lines.txt" - > "$prefix-junction-diff.txt" || true
for f in $xml/TEST-*.xml; do grep -o '<testcase name="[^"]*" classname="[^"]*"' "$f" | sed -E 's/<testcase name="([^"]*)" classname="([^"]*)"/\2.\1/'; done | sort > "$prefix-test-names.txt"
cat "$prefix-summary.txt"; echo "junction diff lines: $(wc -l < "$prefix-junction-diff.txt")"

#!/usr/bin/env bash
# Call-site census of DeterministicMath (batch 2026-09-27-jdk-determinism, pow performance study).
#   census.sh <scratch-dir> [patch...]
# Copies src/main/java and src/test/java of the worktree into <scratch-dir>/src, applies the given patches (e.g.
# ../../full-science-routing.patch for the column), rewrites every DeterministicMath.f( call in src/main/java into
# DeterministicMath.f("File.java:line", ... ) and drops in the census facade of this folder. Then compile and run with
#   SRC=<scratch-dir> RES=<worktree> OUT=<out> ../wbuild.sh compile fluid|column
#   SRC=<scratch-dir> RES=<worktree> OUT=<out> JVM_OPTS="-Xmx2g -Dcensus.out=<file>" ../wbuild.sh run <selectors>
set -euo pipefail
HERE=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
ROOT=$(cd "$HERE/../../../.." && pwd)
DEST=${1:?scratch dir}; shift
rm -rf "$DEST"; mkdir -p "$DEST/src/main" "$DEST/src/test"
cp -r "$ROOT/src/main/java" "$DEST/src/main/java"; cp -r "$ROOT/src/test/java" "$DEST/src/test/java"
for p in "$@"; do (cd "$DEST" && patch -p1 -s --no-backup-if-mismatch < "$p"); done
MATH=src/main/java/com/wormzjl/createcheme/science/math
find "$DEST/src/main/java" -name '*.java' ! -path "*/science/math/*" | while read -r f; do
  grep -q 'DeterministicMath\.' "$f" || continue
  base=$(basename "$f")
  # "File.java:line", or "File.java:line#k" for the k-th call (k >= 2) on the same line
  BASE=$base perl -i -pe '$n=0; s/DeterministicMath\.(\w+)\(/"DeterministicMath.$1(\"$ENV{BASE}:$.".(++$n>1?"#$n":"")."\", "/ge' "$f"
done
cp "$HERE/DeterministicMath.java" "$DEST/$MATH/DeterministicMath.java"
echo "census tree ready: $DEST ($(grep -rho 'DeterministicMath\.[a-z0-9]*("' "$DEST/src/main/java" | wc -l) sited calls)"

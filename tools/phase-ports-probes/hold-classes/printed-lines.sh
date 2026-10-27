#!/usr/bin/env bash
# Compares the printed lines of two G1 runs (gates.sh <prefix>-G1-stdout.txt) with wall-time fields masked: ms=, time="",
# allocatedMB=, bytes=, wall/nanos fields and log timestamps. Lines are compared as a multiset (test classes may print in
# another order). Usage: printed-lines.sh <A-stdout> <B-stdout> [out-prefix]; prints the count of compared lines and of
# lines only in A / only in B, and writes them to <out-prefix>-printed-diff.txt when given.
set -u
mask(){ tr -d '\r' < "$1" | grep -v '^<?xml \|^\s*<testcase\|^\s*</\?testsuite\|^\s*<properties\|^\s*</properties\|^\s*<property \|^\]\]></system-\|^\s*<system-' \
  | sed -E 's/(ms|wallMs|millis|allocatedMB|bytes|nanos|Nanos|wallNanos|elapsed)=[0-9.eE+-]+/\1=*/g; s/time="[0-9.]+"/time=*/g; s/^\[[0-9:]+\] //; s/ [0-9.]+ ?ms\b/ * ms/g; s/ in [0-9.]+ ?s\b/ in * s/g; s/^(one placement, median ms by world size:).*/\1 */' \
  | perl -pe 's/reasons=\{([^}]*)\}/"reasons={".join(", ",sort split(m{, }, $1))."}"/ge' | LC_ALL=C sort; }
a=$(mktemp); b=$(mktemp); mask "$1" > "$a"; mask "$2" > "$b"
onlyA=$(LC_ALL=C comm -23 "$a" "$b" | wc -l); onlyB=$(LC_ALL=C comm -13 "$a" "$b" | wc -l)
echo "lines A=$(wc -l < "$a") B=$(wc -l < "$b") onlyA=$onlyA onlyB=$onlyB"
if [ $# -ge 3 ]; then { echo "# only in $1"; LC_ALL=C comm -23 "$a" "$b"; echo "# only in $2"; LC_ALL=C comm -13 "$a" "$b"; } > "$3-printed-diff.txt"; fi
rm -f "$a" "$b"

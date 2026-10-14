#!/usr/bin/env bash
# Seeds clean in report A and not in report B (worse), and the reverse (cleared). Usage: compare.sh <A-report> <B-report>
clean(){ awk '/^SEED /{if($0 ~ /holds=0 classes=\[\]/) print $2}' "$1" | LC_ALL=C sort; }
echo "worse: $(LC_ALL=C comm -23 <(clean "$1") <(clean "$2") | sort -n | tr '\n' ' ')"
echo "cleared: $(LC_ALL=C comm -13 <(clean "$1") <(clean "$2") | wc -l)"

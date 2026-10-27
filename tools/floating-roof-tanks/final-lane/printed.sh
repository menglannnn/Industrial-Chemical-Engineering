#!/usr/bin/env bash
# printed.sh <results dir> <out file>: every test's system-out lines, class by class in name order, the class name
# (package dropped, so createcheme and icheme runs pair) in front of each line.
d=$1; out=$2; : > "$out"
for f in $(ls "$d"/TEST-*.xml | sort -t. -k1); do
  c=$(basename "$f" .xml); c=${c##*.}
  awk '/<system-out><!\[CDATA\[/{f=1; sub(/.*<system-out><!\[CDATA\[/,"")} /\]\]><\/system-out>/{sub(/\]\]><\/system-out>.*/,""); if(length($0))print; f=0} f' "$f" | grep -v '^$' | sed "s|^|$c |" >> "$out"
done
wc -l < "$out"

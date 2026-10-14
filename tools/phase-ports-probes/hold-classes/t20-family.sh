#!/usr/bin/env bash
# t20: the first-interval family of a campaign report (first-interval.js's definition: the first hold's committed tick is a
# placement tick of the seed's layout), as a comma-separated seed list. Usage: t20-family.sh <report>
set -u
awk '/^SEED /{seed=$2;split("",t);f=0;next} /^  layout /{for(i=2;i<=NF;i++){split($i,a,":");t[a[1]]=1}}
     !f&&/^  HELD /{f=1;match($0,/committed=([0-9]+)/,m);if(m[1] in t)print seed}' "$1" | paste -sd, -

#!/usr/bin/env bash
# t21: per-seed Newton iterations and first hold of two one-worker reports (alone.sh), side by side.
# Usage: t21-perseed.sh <A-report> <B-report>   prints: seed itA itB holdsA holdsB
set -u
ext(){ awk '/^SEED /{s=$2; h=$5} /COUNTERS_SEED/{for(i=1;i<=NF;i++)if($i~/^newtonIterations=/){split($i,a,"=");print s, a[2], h}}' "$1" | sort -n; }
join <(ext "$1") <(ext "$2") | awk '{printf "%5d %10d %10d %-9s %-9s %6.2f\n",$1,$2,$4,$3,$5,($4+1)/($2+1)}'

#!/usr/bin/env bash
# Runs one set of MiniLauncher selectors for every build x runtime given, via xrt.sh.
#   matrix.sh <tag> "<builds>" "<runtimes>" <selectors...>   logs: logs/xrt/<tag>-<build>-<runtime>.txt
set -uo pipefail
HERE=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
tag=$1; builds=$2; runtimes=$3; shift 3
mkdir -p "$HERE/logs/xrt"
for b in $builds; do
  for r in $runtimes; do
    echo "== $tag $b $r"
    bash "$HERE/xrt.sh" "$b" "$r" "$HERE/logs/xrt/$tag-$b-$r.txt" "$@"
  done
done

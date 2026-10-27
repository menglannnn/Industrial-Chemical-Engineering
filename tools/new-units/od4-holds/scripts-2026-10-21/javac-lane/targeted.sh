#!/usr/bin/env bash
# t285: the targeted classes (/d/tmp_285/targeted.txt, paths under src/test/java) compiled and run on export <name>; summary to out/targeted-<name>.txt
set -u
name=$1; files=$(tr '\n' ' ' < /d/tmp_285/targeted.txt); sel=$(sed -e 's|\.java$||' -e 's|/|.|g' /d/tmp_285/targeted.txt | tr '\n' ' ')
bash /d/tmp_285/scripts/lane.sh compile "$name" $files | tail -3
bash /d/tmp_285/scripts/lane.sh run "$name" $sel > /d/tmp_285/out/targeted-$name.txt 2>&1
grep -E "M3RUN (SUMMARY|FAILED)" /d/tmp_285/out/targeted-$name.txt | cut -c1-300

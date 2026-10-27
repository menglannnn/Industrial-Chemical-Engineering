#!/usr/bin/env bash
# m23: the one alone batch. The new C/F holds against m20/m21 (cold 285, 651, 866), one worker, on the worktree at 8be1efef,
# then the same seeds with OD4 off (scratch copy D:/tmp_m23/od4off = git archive 8be1efef + od/rule-off-od4-half-link.patch).
set -u
F=D:/Minecraft/Modding/1.21/CreateChemE/tools/unified-thermo-port-forms
WT=D:/Minecraft/Modding/1.21/CreateChemE/.claude/worktrees/stoic-bell-ae6019
S=${SEEDS:-285,651,866}
echo "== alone $(date +%H:%M:%S)"
THREADS=1 HARNESS=$F/hold-fuzz-cold DISTS=random EXTRA=-PholdFuzzCold=true OUT=$WT/build/m23-logs/06-alone bash /d/tmp_fl/scripts/fuzz.sh 06-alone-cold $S
echo "== od4off $(date +%H:%M:%S)"
COPY=D:/tmp_m23/od4off THREADS=1 HARNESS=$F/hold-fuzz-cold DISTS=random EXTRA=-PholdFuzzCold=true OUT=D:/tmp_m23/od/od4off bash /d/tmp_fl/scripts/fuzz-copy.sh 09-od4off-cold $S
echo "== done $(date +%H:%M:%S)"

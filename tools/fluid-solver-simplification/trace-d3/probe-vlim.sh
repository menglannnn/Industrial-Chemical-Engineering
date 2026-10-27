#!/usr/bin/env bash
# t42: extends the pass probe's node line (../trace/patches/probe-pass-trace-on-573b050b.patch) with each node's free-water
# volume and velocity limit (model.velocityLimit), on a lane export only: bash probe-vlim.sh <export-name>, then recompile.
set -eu
F=/tmp/d40-${1:?export name}/src/main/java/com/wormzjl/createcheme/science/fluid/network/PassiveStepSolver.java
sed -i 's/ liqV=%.5g vapV=%.5g V=%.4g layout=%s%n",node,r.id(),r.kind(),r.elevation(),s.pressure(),s.temperature(),s.mass(),s.liquidVolume(),s.vaporVolume(),r.inventory().volume(),/ liqV=%.5g vapV=%.5g watV=%.5g vlim=%.5g V=%.4g layout=%s%n",node,r.id(),r.kind(),r.elevation(),s.pressure(),s.temperature(),s.mass(),s.liquidVolume(),s.vaporVolume(),s.waterVolume(),model.velocityLimit(s),r.inventory().volume(),/' "$F"
grep -q 'watV=%.5g vlim' "$F" && echo "probe-vlim applied to $F"

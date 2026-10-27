#!/usr/bin/env bash
# t21: minimizations of the two smallest new worse worlds of ff13 (world defaults 951, 322), keeping E6. From the worktree root.
set -u
H=tools/phase-ports-probes/hold-classes
SLICES=120 bash $H/t21-minimize.sh logs/ff15-worse min d 951 E6
SLICES=120 bash $H/t21-minimize.sh logs/ff15-worse min d 322 E6

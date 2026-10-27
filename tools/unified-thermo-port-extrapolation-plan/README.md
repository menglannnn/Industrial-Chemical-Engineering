# unified-thermo-port-extrapolation-plan

Purpose: read-only probe of what the property models give outside their declared data ranges, as input to
`documentation/2026-09-24-coolprop-low-temperature/RANGE_EXTRAPOLATION_PLAN.md` (batch
`2026-09-24-coolprop-low-temperature`, branch `claude/unified-thermo-port`, commit `1b75ce08`). No tracked code was
changed or removed; nothing here needs re-attaching.

Contents:
- `probe/ExtrapolationProbe.java`: one `main` class in the default package. It prints, with no code change:
  IF97 Region 1 (`WaterRegion1.fill` by reflection, no range check) from 273.16 K down to 100 K at 0.1, 1 and 10 MPa
  against a hold at 273.16 K; the pilot package's PR78 liquids (residual part) of each component and an LNG from 120 K
  down to 30 K, and methane vapour past the Soave alpha zero (1200 to 5000 K); every `log_table` viscosity of the pilot
  and bundled `tjl20_methane_nitrogen` packages continued past both ends (liquid ln mu linear in 1/T, vapour ln mu
  linear in ln T, end-segment slopes); the SR6 water viscosity power sum below 253.15 K; the IAPWS dilute-steam
  viscosity formula below 273.16 K; Wagner-Pruss continued below 273.16 K against the IAPWS R14-08 ice sublimation
  pressure; the R14-08 ice Ih melting pressure against a linear Clapeyron line.
- `scripts/compile.sh [rev]`: exports `src/main/java` of `rev` (default HEAD of the worktree) with `git archive` into
  `/tmp/extrap-plan/`, compiles it with javac (JDK 21, no Gradle), then the probe. Adapted from
  `tools/unified-thermo-port-m3/scripts/index-compile.sh`.
- `scripts/run.sh`: runs the probe (`java -Xshare:off -Xmx6g`) against those classes and the worktree's
  `src/main/resources` (the bundled material catalog).
- `logs/probe-logs.tar.gz`: `run1.txt`, the one probe run at `1b75ce08` (2026-10-16).

How to run (check first that no Minecraft dev client is live; the lane is not Gradle):

    bash scripts/compile.sh          # prints "compile rev=... main=0 probe=0"
    bash scripts/run.sh > run.txt

Findings used by the plan: IF97's own formula passes its check to 100 K but diverges below about 230 K (cp 911 J/mol K
and 664 kg/m3 at 200 K), so the plan holds water at its 273.16 K values; PR78 liquids are smooth to 30 K and the
cubic needs no guard past the alpha zero; liquid viscosity tables that end at Tc have end slopes up to 4e6 K and must
be held above their top; the dilute-steam formula has a pole near 134.5 K; Wagner-Pruss continued gives the
supercooled-liquid vapour pressure (1.10 times the ice value at 263 K).

## Index record (moved from tools/INDEX.md on 2026-10-26)

Purpose: Probes behind `RANGE_EXTRAPOLATION_PLAN.md`: PR78 liquids to 30 K, IF97 and SR6 below 273.16 K, viscosity-table continuations, ice melting line and sublimation pressure against IAPWS R14-08 (one index-lane run at `1b75ce08`)

Batch: 2026-09-24-coolprop-low-temperature

Status: Done 2026-10-16. Index: [../INDEX.md](../INDEX.md).

### Entry points

`README.md`

### History

- Done 2026-10-16 (plan approved by the owner 2026-10-16, all options A)

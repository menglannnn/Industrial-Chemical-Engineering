# staging: proposition A of M3_COLD_ARRIVAL_TRACE.md (probed, not applied)

- `a-own-split.patch` (against `76e87740`; `git apply --check` passes on that commit's two files): an empty bag's first
  layout (`PassiveStepSolver.arrivingPhases`, D54) is its own split (D26) of what arrives, the engine's PH answer at the
  delivered enthalpy and the ambient pressure (`FluidThermodynamics.equilibriumPH`, made public) closed by
  `InventoryEquilibrium.split` on an inventory of what arrives (wall work from a zero bag volume: PH while slack, UV at the
  capacity once it overfills); the TP flash at the mass-weighted temperature stays the fallback. Probed on the index lane
  (logs 05 to 09); never applied to the worktree. Apply only on the owner's go.

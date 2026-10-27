# Process-control adaptation review

Batch 2026-10-26-multi-machine-framework, 2026-10-27. Owner: adapt this branch to `claude/process-control-fixes` (batch 2026-10-26-process-control, `fa62fd81`, unmerged; the same base as this branch, `600549d9`, 0.13.2).

## 1. Expected

A Control Valve on a boundary line is the assembly's valve whatever module it carries (none: fully open; a pressure module: PC; a flow module: FC). The structure stage counts the valve; its job is the next stage (plan 14.4). A module, a target or a legacy conversion is a setting, not a structural change: a validated report stays.

## 2. What process-control changes that this branch reads

- Valves: a sealed `FlowControl.Valve` holds the legacy `PressureValve` and the new `ControlValve` (source NONE, PRESSURE, MASS_FLOW or VOLUME_FLOW; its sensor; a revision). Every new fitting and one-block valve is `ControlValve.unconfigured()`; a placed pressure valve converts once on load into a valve with a pressure probe (`reconcileLegacyValves`). The item reads "Control Valve".
- Boot drums: `LIQUID_OUTLET` becomes the boot's hydrocarbon port and `BOOT_DRAIN` its water port. The receiver's links already use them so (oil to the reflux pump, sour water through its valve): no change.
- Sensors: an accessory on a pipe (the pipe stays passive) or a module on a valve. The walk passes them, as plan 14.3 says for instruments: no change.
- A valve's bore is its host pipe's (`withActuatorArea`): a resized line's valves follow its bore (line resize, `61d6dd2e`): no change.

## 3. What this branch did on that code

`LevelGround.piece` named the class (`control() instanceof FlowControl.PressureValve`). On the merged code every placed valve (a bare `ControlValve`) reads as a plain pipe: every valve slot missing, no column ever complete. A valve fitted on a checked line would not revert it (same code as a pipe), and the legacy conversion on the first load would revert every validated column (its valves turn into plain pipes). The GameTests' valve wait named the same class.

## 4. Change (`d5710373`)

The walk reads a fitted pipe by the law it compiles through, `Device.hydraulicKind()` (VALVE, PUMP, COMPRESSOR), which both builds define. The class test goes; nothing branches on valve types. `LevelGroundTest` pins the reading; the GameTest wait uses the same test. Comments say "control valve".

## 5. Verification

- This branch: assembly, blueprint and `LevelGroundTest` 44/44; the fluid GameTests compile.
- A scratch merge of `d5710373` with `fa62fd81` (removed after): the affected unit tests (assembly, blueprint, column structure, line resize, fittings, process-control's `FlowControlTest` and `ProcessOutletTest`) pass, with two scratch-only assertions (a bare and a flow-module `ControlValve` read as the control valve); fluid GameTests 30/30, process-control's modular-valve and phase-outlet tests included; the assembly plant validates complete with bare control valves. Logs: `tmp/pc-adapt/`.

## 6. For the merges

- The two branches conflict only in `FluidFacts.java` (one comment) and `compat/fluid/schema-2/` (both add an image). Every other shared file merges clean.
- Fluid facts schema 2 is claimed three times: main 0.14.0 (pipe laying), process-control, and this branch (H1's basis revision, `660650cd`). The second branch to merge takes schema 3, the third schema 4; each records its image under its new number and reads the earlier ones.

## 7. Valve modules (decided)

The structure check counts the valve body only (as built, plan Q5: no placement rules this stage). Option: also show each boundary valve's module against its job (PC on the off-gas valve; FC on the feed, steam and draws; LC waits for process-control's cable batch) as a yellow hint, not a fault. Owner, 2026-10-27: do not check for modules. The check counts the valve body only, as built.

Code guide: no change now (the assembly chapter is written at W8).

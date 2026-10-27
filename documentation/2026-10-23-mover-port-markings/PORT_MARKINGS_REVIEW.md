# Pump/compressor port arrows — review

2026-10-23; code `07e45392`, delivery `1730cf14`, branch `codex/mover-port-markings`.

## Expected behavior

- Fixed arrows show the registered design direction: suction into the constructed machine, discharge out. At the existing defaults this is the pump's requested 0.01 m³/s or a compressor's 0.05 m³/s direction; the marking does not change the accepted flow or the existing off/phase protections.

## Previous behavior and implementation

- Unmarked nozzles made ports hard to distinguish, especially perpendicular/elevated connections and the centrifugal compressor's below-table ports. No hydraulic defect was changed.
- `MoverPortMarkings.surface` builds four word-free yellow/black arrows around each SUCTION/DISCHARGE neck, using the same seven-bit size codes and equipment transform as its model. Existing materials supply solid paint texels; no new bitmap asset or font.
- `MoverPortMarkings.pipe` paints the adjoining pipe's actual exposed axial planes. It merges coplanar hub/neck/UV pieces and chooses four radial viewing sides, so it avoids duplicate arrow rows, floating decals on bends/reducers and painting on glass.
- `FluidDeviceBlockEntity` derives MoverPorts (two bits per face: none/suction/discharge) from loaded neighboring constructed-nozzle roles. Changes participate in geometry refresh/sync. This field is rebuilt, not saved; block update tags carry only static geometry metadata.
- Both model caches include the relevant size/role keys. Equipment arrows remain in both still and moving-part lists; pipe arrows are added to solid geometry. Pipe material bindings add the existing guard sprite. Coolant ports, one-block models and item models receive no direction marking.
- All process settings, schedules, physics, collision and formats are unchanged (checkpoint 15 / unit 8; current networking contracts retained).

## Verification and delivery

- Final `gradlew.bat jar --max-workers=4`: PASS. `git diff --check` clean; tracked ignored file list empty. No tests added/run and no automated GUI/in-game checks.
- Software rendering: 24 valid cases, 52 views plus preview sheet: centrifugal and split-case pumps, both compressor designs; four headings; drawn pipe OD 0.0625 and 1 m. Every case emitted 32 nozzle and 32 adjoining-pipe marker polygons. Actual production mesh/textures and back-face/depth handling were used; preview visually checked.
- Owner checks after integration/restart: arrow direction at all process ports; resize pipes; below-table ports; removal/unload/reload; paint/animation states; corners/junctions/sight glass beside a machine. Current running integration client was not stopped or modified.
- Guide: CODE_GUIDE_ADDENDUM.md and owned equipment/client-gui/fluid-runtime branch addenda snapshot 1730cf14. Canonical main chapter consolidation remains due at a future main merge.
- Tool cleanup: offline renderer adapted from tools/new-units/UnitRender and sheet script live in tools/mover-port-markings/; never tracked or detached, no new Gradle switch/harness. Logs archived there; renders in this batch.
- Delivered feature branch only, no target/main merge or mod_version bump. Main merge checks and owner version choice remain due.

Superseded geometry, 2026-10-23: SINGLE_ARROW_REVIEW.md and code e612cff3 remove equipment-side markings; the current contract and renders show a single pipe-side set only. Delivery 763939e8 remains unmerged.

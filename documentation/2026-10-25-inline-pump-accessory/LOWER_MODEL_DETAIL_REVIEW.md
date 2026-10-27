# Lower assembly model review

Implementation8a910a20, codex/inline-pump-accessory;2026-10-25. Previous feature delivery1444b61a.

Expected appearance: the owner photograph has curved same-level port passages feeding a lower impeller chamber, open support columns and a rectangular base. Sundyne's LMC has a distinct geared barrel and horizontal casing split above its process body. These are passive model details; original hydraulic datum/storage/control behavior remains.

Previously the lower assembly read as a horizontal barrel with an upright motor. Adding bolts alone did not correct that form. InlineMoverGeometry now builds an elliptical scroll, curved inlet/outlet passages, hanging chamber, bolted top cover, four-column open base and drain. The compressor gains a separate lower gear barrel, wide gasket split, ribbed upper case, inspection cap and connected oil canister.

- The joints are compact paired machine flanges next to the body, with normal pipe tails/reducers beyond. hasBoundaryFitting excludes inline movers, avoiding an extra flange pair at the host block boundary; adjacent non-mover fittings/equipment still request their own normal joints.
- The maximum-bore sweep exposed a passage wall covering its flange. Axial passage width is now constrained behind the mating plane while the body grows laterally. Machine flange proportions continue beyond one block so bolts/rims remain visible at1m bore. Exterior bore-wall duplication was removed at the flange seam.
- Sources: owner blue photo; CNP TD manufacturer catalogue pp13/15/16; TD orthographic view; official Sundyne LMC311 photograph and brochure p7 cutaway. See MODEL_REFERENCES.md. The pump photo's exact maker is unverified.
- Final verification: InlineMoverAccessoryTest4 + PhysicalFluidTopologyTest16 =20 tests, zero failures/errors/skips; jar successful, client closed. Existing test checks finite native geometry, winding, headings and size extremes. No new test or changed gate scope.
-98 native software views: two types, four headings, three bore sizes, three full angles, lower closeups and item looks. Reviewed both full sheets, default lower/item looks and maximum-bore flanges. This is software geometry verification, not in-game lighting/chunk-sync proof.
- No Minecraft run or desktop input. Owner gameplay checks from INLINE_PUMP_REVIEW.md remain applicable. Full quick/slow suites remain mandatory before a later merge; none claimed for this model-only delivery.
- Canonical tooling: tools/inline-mover-accessories (renderer/sheets/evidence). No detached tracked code or custom Gradle switches/tasks. Evidence: lower-detail-verification.json and logs/lower-model-detail-logs-2026-10-25.tar.gz. Prior53-test feature evidence remains separate.
- Changelog and code guide updated (equipment, client-gui, verification/overview); runtime physics chapter needs no additional contract change. Formats19/12, accessory1, fluid-14/instruments-1 and mod0.11.2 retained. Feature branch delivered; no merge/version bump.

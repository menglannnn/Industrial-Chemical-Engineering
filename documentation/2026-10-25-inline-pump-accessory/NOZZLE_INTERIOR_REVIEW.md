# Nozzle interior review

Implementation52b9f8b0; codex/inline-pump-accessory;2026-10-25.

Expected: the open-looking nozzle rim shows a recessed opaque process throat. The simplified machine has no modeled internal cavity, and should not expose unrelated casing surfaces or scenery through its port.

Previously processJoint built flange rings and their bore walls but left the inner end open. Looking down it exposed the outside of the curved casting and background through gaps between model primitives. This was an incomplete interior, not chunk clipping.

InlineMoverGeometry.processJoint now terminates the visible throat with an outward-facing OPENING disk0.001m ahead of the rear flange plane. It is recessed by nearly the full paired flange thickness, meets the existing bore radius, and remains ahead of cast walls. Pump and compressor use the same path; no duplicated bore wall or coplanar front face is added.

- Verification: the one existing eligibilityAndMeshesCoverBothAxesHeadingsAndDiameterExtremes test passes (zero failures/errors/skips); jar succeeds with client closed. No new tests or widened suite. Prior20-test evidence remains separate in lower-detail-verification.json.
-100 software views generated: prior98 plus two equipment-only nozzle/lower closeups. Reviewed pump item, both model sheets and port closeup; the exposed casing/background is replaced by a continuous dark recess. Four headings, min/default/max bore and multiple angles retained.
- No in-game check: this drawing-only correction is judged by native software rendering/winding. No lighting/chunk-sync claim; no client launched.
- Tooling: tools/inline-mover-accessories, nozzle-interior-verification.json, logs/nozzle-interior-logs-2026-10-25.tar.gz. No detached tracked source/custom Gradle switch or gate-scope change. No physics, collision, settings, format, protocol or version change; hydraulic flow remains unaffected by visual throat closure.
- Batch index, equipment/overview/verification code-guide entries and changelog updated. Feature branch delivered without merge. Full quick/slow qualification remains due before a later merge.

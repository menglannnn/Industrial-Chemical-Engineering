# Liquid-level gauge accessory tools

Batch `2026-10-26-liquid-level-gauge-accessory`, implementation `b1477c09`. Native software preview and deterministic pixel icon sources; nothing in this folder is needed to build the product. No tracked probe was detached, no task/exclusion added; no removal/re-attachment patch applies.

With the feature worktree compiled, run `run-render.ps1 -SourceRoot <worktree>`. It uses JDK21/cached Gson, actual ShapeRasterizer surfaces/custom glass and TankDisplay colour mixing. Eight views show the uninstalled, ghost, installed and empty states; assemble.py creates level-gauge-accessory.png. All output/classes go to `<worktree>/tmp/level-gauge/`, whose folder must be listed in its tmp/INDEX.md. Rendering approximates game lighting/translucency and is not an in-game check.

LevelGaugeIcon.java is the editable code-native source for the tracked32x32 item icon. Compile it to `<worktree>/tmp/level-gauge/icon-classes`, then run it with the desired tracked item texture path as its sole argument. It generates a new bitmap, not an edit of a user image.

Focused command (client closed): `gradlew test --tests '*LiquidLevelGaugeTest' --tests '*GaugeAccessoriesTest' --tests '*EquipmentRefreshTest' --tests '*ShapeRasterizerTest.tankGauge*' jar --max-workers=4 --console=plain`. Final24 pass/package succeeds. Logs/XML archived under logs/. Current working evidence remains in the unmerged worktree tmp folder until the owner's merge/cleanup stage.

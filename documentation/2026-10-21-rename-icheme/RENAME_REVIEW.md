# Rename CreateChemE → Industrial Chemical Engineering (IChemE): review

Branch `claude/rename-ichem-mod-30e50c` from `144321e7` (0.10.1). Inventory and options: [RENAME_PLAN.md](RENAME_PLAN.md).

## Owner decisions (2026-10-21)
- R1 Scope A+B+C: display name, mod id `icheme`, Java package `com.wormzjl.icheme`.
- R2 No backward compatibility: 0.10.1 worlds lose this mod's blocks; tested on a fresh world.
- R3 The name IChemE is kept despite the Institution of Chemical Engineers' acronym.
- R4 GitHub repository `wormzjl/CreateChemE` → `wormzjl/Industrial-Chemical-Engineering` (name free, repo public).
- R5 Local folder `D:/Minecraft/Modding/1.21/CreateChemE` kept: renaming it touches 37 worktrees, 144 tool scripts and
  61 documents with absolute paths, 13 memory files, 23 Claude session folders and the IDE configurations.

## What changed
- `git mv` of every `createcheme` path (package roots in 4 source sets, `assets/`, `data/` in main, test and
  examples, 3 GameTest namespaces, the mcpCompat mixins file) and of `CreateChemE.java` / `CreateChemEClient.java`.
- Text replace `CreateChemE` → `IChemE`, `createcheme` → `icheme` in 865 tracked text files; `CHANGELOG.md` history
  and `AGENTS.md:80` (folder path) left as they were. `mod_name=Industrial Chemical Engineering`; README, AGENTS title.
- Neural artifacts (`-text`, byte-hashed) rebuilt from HEAD with `sed -b` (Git Bash `sed -i` drops CRs). The sidecar's
  `packageId` is `icheme:…`, so `pipelineSha256` = sha256(sha256(payload):sha256(sidecar)) moved `609be438…` → `80dda6de…`
  (the formula reproduces the old value); `regrouped-model-provenance.json` updated to match.

## Verification (baseline: final lane fl8 at `c7b883cb`, same code as 0.10.1)
- Compile of main, test, fluidGameTest and mcpCompat: green.
- Full `test`: 1 572 tests (baseline 1 572), 3 failures, all identity pins: `PilotCryogenicCatalogTest` (8 scientific
  revisions + the pilot's hash the records' `icheme:` ids; every physics and spine fingerprint kept its value) and
  `LegacyNetworkPathPinTest` (its digest hashes refusal class names and messages; with `icheme` mapped back to
  `createcheme` in a reverted probe the digest is the old `98a1af11…` bit for bit). Re-pinned with notes; 9/9 green.
- Chain-100 exact: max deviation 0. Fluid GameTests: 31/31. Asset refs: 44/44 resolve. No campaign:
  a rename moves no physics.

## Tools and code guide (done 2026-10-21, main checkout, git-ignored)
- 86 live tool files moved to `icheme` (CR bytes kept, backups checked): `unified-thermo-port-forms/` hold-fuzz-cold,
  hold-fuzz-bundled, harness; `fluid-solver-simplification/quick-lane.sh`; `unified-thermo-port-m5/d40/` run-touching
  and classes lists; `unified-thermo-port-test-world/` presets, MCP drivers, index lane, datapack ids (`p6_pilot/data/icheme`).
  Kept: absolute folder paths, the `createcheme_bench` datapack name (level.dat enables it), detached probes and patches.
- d40 javac lane: Create's catnip (ponder jar) added to its main classpath; main had not compiled there since
  `addaa0ab8` (EquipmentGhost, 2026-10-16). Hold-fuzz cold and bundled compile against `8033aed8`: main=0, test=0.
  `forms/harness/HoldFuzzTest.java` fails on older `Pump`/`Compressor` drift, not the rename (README note).
- Code guide (9 files: links, names, title, each Updated line); notes in 4 tool READMEs and `tools/INDEX.md`.

## Merge (2026-10-21)
Merged to `main` as 0.11.0 by fast-forward (`138f4993`), not pushed; code guide snapshots set to it. Main-checkout
local sources moved too: `tools/development.gradle`, 25 `tools/neural`, 4 `benchmarks/` probes, 8 ignored V3 probes in
`src/test/java` (now under `icheme/`). Those V3 probes and benchmarks fail `compileTestJava` there on APIs removed
by 0.7.2 (`8a9278b7`), not on the rename.

## Open
- The lane worktree `coolprop-multiphase-thermo-37f6b0` keeps a stale `tools/` copy and an old tree: refresh before its next run.
- The test world template holds `createcheme:` blocks: regenerate it before its next in-game session.
- Push of `main` (18 commits ahead of `origin/main` at `2e28a20a`); GitHub renamed, `origin` re-pointed 2026-10-21.

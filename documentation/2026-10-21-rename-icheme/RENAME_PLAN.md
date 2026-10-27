# Rename CreateChemE → Industrial Chemical Engineering (IChemE): inventory

Surveyed 2026-10-21 on `claude/rename-ichem-mod-30e50c` at `144321e7` (0.10.1). Nothing changed yet.
867 tracked files contain `createcheme`. The work splits into three tiers; the owner chooses which to do.

## Owner decisions
1. Scope: tier A only (display name), A + B (mod id), or A + B + C (Java package).
2. Mod id: `icheme` proposed. Tier B removes every `createcheme:` block, item and block entity from old worlds,
   including placed equipment that the 2026-10-18 EquipmentRefresh exception protects. Accept a fresh world, or keep `createcheme`.
3. Name: "IChemE" is the established acronym of the Institution of Chemical Engineers (UK chartered body).
   The 1.20.1 project at `D:/Minecraft/Modding/1.20.1/IChemE` is Immersive Chemical Engineering (`immersivechemical`). Its mod id does not clash.
4. GitHub name: `IChemE` or `Industrial-Chemical-Engineering`.
5. Local folder `D:/.../1.21/CreateChemE`: keep it (recommended) or rename it last.

## Tier A: display name (no save impact)
- `gradle.properties`: `mod_name`, and `mod_description` if wanted. `settings.gradle`: `rootProject.name`.
- `src/main/resources/pack.mcmeta` description; `src/mcpCompat/resources/META-INF/neoforge.mods.toml` displayName.
- `en_us.json:678-679` and `V3ClientPresets` preset folder `<game>/CreateChemE/{column,mixture}preset` (+ `V3ClientPresetsTest:38`).
- `README.md`, `AGENTS.md` title, `CHANGELOG.md` header, and comments such as `ExtremeTopologyIslandTest:124`.

## Tier B: mod id `createcheme` → `icheme`
- `gradle.properties` `mod_id` (the jar name follows it); `CreateChemE.MOD_ID`.
- `build.gradle`: `mods { createcheme, createcheme_fluid_test, createcheme_mcp_compat }`, `loadedMods`,
  gametest namespaces (3), `createcheme.*` system properties (~20), `config/createcheme-common.toml` (l.312).
- Folders: `assets/createcheme` (90 files), `data/createcheme` (109), test resources `data/createcheme` (42),
  `examples/material-override/data/createcheme`, gametest `data/createcheme_{fluid_test,column_test,fluid_benchmark}`.
- Strings in code: config file name, `createcheme_fluid_core` / `createcheme_fluid` saved data, `createcheme_solid_parcel` NBT key,
  `createcheme-cpu-solve-` thread name, `createcheme.fluid.scheduler.verify` property, digest seeds
  (`createcheme-fluid-checkpoint-5`, `createcheme-certificate-graph-identity-2`; no test pins them), model loader ids.
- Resource keys: ~970 lang keys (`gui./block./message.createcheme.*`), `createcheme:` ids in materials JSON
  (`materials-index.json` 94, crude bases, cdu17/pilot-cryogenic fixtures, DWSIM refs), `createcheme_mcp_compat.mixins.json`.
- `MATERIALS.md`, `examples/*.py`.

## Tier C: Java package `com.wormzjl.createcheme` → `com.wormzjl.icheme`
- `mod_group_id`; ~640 files in src/main, test, fluidGameTest and mcpCompat (`git mv` + sed); mixin package;
  `build.gradle` test filters (l.259-380); classes `CreateChemE` and `CreateChemEClient` → `IChemE` and `IChemEClient`.

## Outside the repository
- GitHub: `gh repo rename <name>` (the old URL redirects), then `git remote set-url origin` in the main checkout.
  Rename only after the owner confirms.
- Local docs and tools in the main checkout: code guide (9 files), `tools/` (5,737 files, mostly logs; only live scripts need edits), `.vscode`, `.idea`.
- Renaming the folder breaks the absolute paths of every worktree (`git worktree repair`), the Claude memory key
  `D--Minecraft-Modding-1-21-CreateChemE`, `AGENTS.md:80` and the run configurations.

## Merge tasks
CHANGELOG entry, `mod_version` bump (minor if tier B), code guide README and chapters, `INDEX.md` row,
gates run once, one in-game check on a fresh world (assets and lang resolve under the new namespace).

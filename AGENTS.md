# Agent rules for CreateChemE

Standing rules from the project owner. They apply to every agent working in this repository (Claude, Codex, subagents) for every task, unless the owner states otherwise for a specific task.

## Save and data compatibility (recorded 2026-09-23)

- No compatibility work at the current development stage. A breaking change to a save format, checkpoint, wire protocol or material data set is tested on a fresh world.
- Never test against an old world, never write a migration, absent-field normalisation, or legacy-save test gate for such a change.
- New formats must still validate what they read and round-trip clocks, material, pending events and any optimisation state they carry.
- Exception (owner request 2026-10-18: "Add a compatibility system so when loading an old save updated model is replaced by new ones"): a placed equipment structure whose cells an older layout of its shape placed is rebuilt to the current layout after it loads (`EquipmentRefresh`, `EquipmentStructure.refreshLater`, gated by `EquipmentRefreshTest`). It keeps its origin, paint, nozzle sizes and a drum's registration, and never overwrites another block. The exception covers equipment layouts only; every other format above stays without compatibility work.

## Player-facing updates (recorded 2026-09-23; amended 2026-09-24)

- The simulation engine produces process presentation snapshots, acknowledgements and status changes on its own schedule, roughly every five seconds of online time.
- Send GUI/process presentation data only in response to a client request. Opening a GUI requests the last published snapshot immediately and subscribes that client to scheduled updates while the GUI remains open; closing it ends that subscription. Do not broadcast contents to clients with no active request.
- Opening a GUI or an explicit read request may immediately replay the server's last published snapshot. This is a read of cached data: it must not run a solver, materialise new process state, apply pending input, or advance simulation time. If no snapshot exists, wait for the engine's first scheduled publication.
- Player inputs and changes to a network remain queued engine events. Their acknowledgements and newly calculated state are delivered on the engine schedule; a packet handler, block-entity load or menu open must not force a new process update.

## Process work and the tick loop (recorded 2026-09-23)

- No process-state calculation on Minecraft's per-tick loop. Work is driven by due simulation deadlines, dependency changes and worker completions; the tick hook may only check what is due, drain completions and flush a presentation bucket.
- Simulated time stays online ticks at nominal 20 TPS; no wall-clock or offline advancement.

## Working rules already in force

- Gradle invocations (amended 2026-09-28): at most two at a time on the machine, each in its own worktree, and only when neither measures time (correctness runs that count tests or seeds). A run that measures time (benchmarks, cost gates judged by wall time) runs alone. Never a test suite while a dev client is running. When two runs overlap, cost is judged by counters, not wall time, and a wall-clock budget hold is replayed alone before it is counted.
- Never use bare `git stash` or `git stash pop`; set work aside with a WIP commit or a tagged stash entry restored with `git stash apply`.
- Work only inside your own worktree; never modify another agent's worktree sources.
- Every review or audit is written to `documentation/<batch>/<TOPIC>_REVIEW.md`; plans to `documentation/<batch>/<TOPIC>_PLAN.md`, inside the folder of the batch of work they belong to (see the next section).
- GUI work is verified in the dev client through the langyo/minecraft-mod-mcp bridge (jar in `<worktree>/run/mods`, `.mcp.json` in the worktree root).
- Large solver campaigns run with 8 to 10 worker threads and never overlap another campaign at that size. A run on the second Gradle lane uses at most 4 worker threads.


## Measurement per task: lean (recorded 2026-10-13)

The owner's words: "try to shorten the tests, they are taking too long/too much effort". Every task measures with the least that decides it:

- Forms and variants are compared on the 200-seed quick check (both distributions), at most three forms per task; one is chosen.
- One full campaign (1000 seeds, both distributions, 8 workers) for the chosen form only. Worse seeds are not replayed alone; only new budget-class holds (classes C and F that are not in the baseline) are replayed alone, batched in one run.
- Worse worlds are listed by class from the campaign's classifier; at most the two smallest new ones are minimized and traced.
- References once: the suites (which give the 33 junction lines, the printed lines and MIXED_GAS_COST), chain-100, and the GameTests inside one final `gates.sh` run. No second gates run: the GameTest read is at a fixed online tick (A141) and is deterministic.
- A gate test's proof with the rule disabled runs that one test, not the suite.
- Counters come from the campaign itself; no separate per-seed Newton runs.
- A split of a hold class by mechanism is made only when the task targets that class, on the final code, with the existing classifier script.
- Reports are at most 50 lines.
## Defects: physics first, structure over patches (recorded 2026-09-28)

- Every issue is first reasoned from real-life physics, with simplifications: what flows where, and what the steady state is, in numbers. The report leads with that expectation, then states what the code did instead in plain words, and ends in a proposition for the owner to validate.
- Every fix improves the existing code structure. A fix is placed in the stage it belongs to and absorbs the special cases it makes unnecessary; no isolated patches.

## Thermo validity below the freezing point; no range holds (recorded 2026-10-16)

- The owner's words: "If we have data to the freezing point, the validity of thermo package should be good below that T, that's a general rule for any future implementations."
- A component whose fluid data reaches its freezing (triple) point stays valid below that temperature, both in mixtures and in supercooled or metastable states. An example of a mixture is a solute dissolved in a colder liquid, such as CO2 in LNG.
- A freezing point is therefore never a lower validity bound. Seed windows, domain floors, fluid-only ranges and range clips use the package's own domain floor, not a component's freezing point.
- Amended 2026-10-16, in the owner's words: "it's extremely inconvenient for the system to be put on hold when outside package range, maybe just don't hold it at all (maybe use extrapolated data?)". The plan was approved with "ok keep things lightweight".
- Leaving any data range never holds an island. This covers low or high T and P, and every property model. Properties extrapolate smoothly, with value and slope continuous at the edge. A non-blocking notice on the island's status line reports the extrapolation.
- Only numerical guards remain, where the mathematics breaks: a numerical box, T ≤ 0, no equation-of-state root. They act inside the solvers, as a rejected trial step and a backtrack, never as a hold.
- Holds remain only for capability limits, where the model cannot represent a state.
- Keep these rules lightweight: game-level continuations, not new reference models.

## Small physics simplifications (recorded 2026-10-17)

- The owner's words: "Always keep an eye on this kind of small simplifications in the future which doesn't break the physics too much."
- The example is D36 of batch `2026-09-24-coolprop-low-temperature`: a gas column does not exist, so a vapour weighs nothing in a static column. A gas column weighs 11 to 22 Pa/m at the network's usual pressures, against 4 to 10 kPa/m for liquids. The owner reverted D36 on 2026-10-17 (decision D38) when the cold package showed a CO2-rich gas at 5.4 MPa weighing 1.15 kPa/m, 25 to 30 % of an LNG column.
- Every agent working on physics code watches for simplifications whose physical effect is small at game level:
  - a negligible term;
  - a rule or mode that a constitutive statement replaces;
  - a special case that a coarser model absorbs.
- Each one is proposed to the owner with three things:
  - its effect in numbers at the network's usual states;
  - what it deletes;
  - what it risks, such as a new discontinuity or references that move.
- The owner approves each one before it is implemented. A broad sweep is written as a review; `SIMPLIFICATION_SWEEP_REVIEW.md` in that batch is the first.
- A simplification never breaks conservation of mass, energy or components, and never overrides an owner rule.
- A code comment that records a measured reason for a term counts against removing that term, unless the simplification also removes the cause.

## Documentation, research, tools and versions (recorded 2026-09-23)

- Working documents, research material and offline tooling are local and git-ignored, in three folders of the main checkout (`D:/Minecraft/Modding/1.21/CreateChemE`), which holds the canonical copy:
  - `documentation/`: one folder per batch of work, named `YYYY-MM-DD-topic/` after the day the batch started, plus `reference/` and `repository/`. `documentation/INDEX.md` lists every batch with its status; `documentation/README.md` explains the layout.
  - `research/`: datasets, literature sources, study harnesses and journals, grouped by batch, indexed in `research/INDEX.md`.
  - `tools/`: offline scripts and harnesses, one folder per tool, indexed in `tools/INDEX.md`. `tools/development.gradle` stays at that path.
- Do not add new top-level folders for documents or scripts. `examples/` is tracked and referenced by the build and tests; leave it where it is.
- After every task, update the documentation of its batch in the same session: put the task's documents in the batch folder (create a new dated folder for a new batch) and update the batch row of `documentation/INDEX.md` (and `research/INDEX.md` or `tools/INDEX.md` when those changed) with the status and its date: Implemented (date it reached `main`), In progress since, Planned, Concluded (research only), or Abandoned / Superseded with the reason.
- Documents written in a worktree are copied into the main checkout's `documentation/<batch>/` when the work merges, or when the task ends if it never merges.
- Every merge to `main` adds a `CHANGELOG.md` entry (Keep a Changelog format, one line per batch with its commits and batch folder) bumps `mod_version` in `gradle.properties` and updates the code guide (see "Code guide" below) in the same merge: minor version for a merged batch of work, patch version for a merge that only fixes. `0.1.0` is the state before the changelog existed; `0.2.0` is the solid-phase fluid system merge. Branches record their entry under `[Unreleased]`; the merge turns it into the new version heading.

## Code guide: regular task before every merge to main (recorded 2026-10-15)

- The progressive-disclosure code guide lives in the main checkout at `documentation/reference/code-guide/` (git-ignored, canonical copy there): `README.md` is level 1 (what the mod calculates) and level 2 (code map and routing table); one chapter per subsystem holds the deeper levels (class map, main flow, collapsible detail, owning layer per change, tests that pin it).
- Read `README.md` first when starting work in an unfamiliar area; open only the chapter the task needs.
- Before every merge to `main`, as a regular merge task beside the `CHANGELOG.md` entry and the `mod_version` bump: update the chapters whose code the batch changed (new, renamed or removed classes, changed flows, contracts, defaults, formats and versions, Gradle tasks and gates), set each touched chapter's snapshot line to the merge commit and date, and add a chapter plus a README row for a new subsystem. Facts come from the code on the merging branch, not from plans.
- A batch that changes no code covered by the guide records "code guide: no change" in its review.

## Test classes and tools after a batch (recorded 2026-09-24)

- Before a batch merges, everything it created for measurement or investigation is sorted. What stays in the code: tests a gate task runs, code the product needs, and harnesses a documented Gradle task runs. Everything else leaves the code: one-off probes, campaign and analysis scripts, in-game rigs, instrumentation and baseline patches, run configurations and Gradle switches that only served them.
- Detached material is stored in the main checkout under `tools/<tool-folder>/` (git-ignored, one folder per tool, indexed in `tools/INDEX.md`), never left scattered in `documentation/` batch folders or in a worktree. Each folder has a `README.md` stating its purpose, the batch it served, how to run it, and, for code removed from a tracked path, the commit that removed it and how to re-attach it (source files plus a patch against that commit, so `git show <sha>:<path>` and the stored copy agree).
- The batch's review names the tool folders that hold its detached material, so an agent finds them from `documentation/INDEX.md` or `tools/INDEX.md`.
- A cleanup neither widens nor narrows a gate suite. The gates are re-run after it and recorded in the batch's review, with a `CHANGELOG.md` line for the detached code.

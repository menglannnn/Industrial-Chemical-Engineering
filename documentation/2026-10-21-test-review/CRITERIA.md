# Test review criteria (2026-10-21)

Owner request: "review all the tests, only keep the ones necessary and archive the rest". Tree: `main` at 138f4993 (0.11.0).
A test class is necessary when it is the ONLY guard of at least one of these. Name the criterion.

- K1 Conservation: mass, energy, component or element balance of a production path.
- K2 Format: a persisted or wire format (checkpoint, unit/island format, packet codec, NBT, datapack/JSON loader):
  validation of what it reads, and round-trip (AGENTS.md requires both).
- K3 Owner rule or owner decision encoded as a gate (AGENTS.md rules: engine-owned presentation, no process work on
  the tick loop, no range holds / C1 extrapolation, pipe spacing, equipment refresh exception, 100 m/s cap,
  hold-class worlds, D-numbered decisions the code guide or a comment names as a gate).
- K4 Reference result: an external reference (literature, DWSIM, CoolProp/Helmholtz, Holland, chain-100, CDU17) or a
  pinned regression. One pin per reference is enough; extra pins of the same reference are A2.
- K5 Determinism: bitwise identity, DeterministicMath, ordering independence.
- K6 Core contract: observable behaviour of a production class that no other kept test covers and whose breakage
  would silently corrupt the game (wrong numbers, lost material, crash on load).

Archive when one of these holds (name it):
- A1 Probe, diagnostic, investigation, profile or decision study (prints, weak or no assertions).
- A2 Duplicate: a kept test covers the same path and the same assertion (name the kept test).
- A3 Implementation detail: pins iteration counts, internal ordering, private intermediate values, rather than a
  result, unless an owner decision made it a gate (then K3).
- A4 Timing or benchmark in the unit suite.
- A5 Legacy: tests a path, model or helper that production no longer uses.

Verdicts: KEEP, ARCHIVE, or TRIM (keep the class, archive listed methods; use only when most methods are A2/A3).
Helpers (non-Test classes) and test resources: KEEP if any KEEP/TRIM test uses them, else ARCHIVE.
When in doubt between KEEP and ARCHIVE, KEEP and say why in the reason.

# WTI pumping-rate hold tracing plan

2026-10-24. Owner resumed tracing immediately. Reported trigger: WTI pumping-rate setting increased from 1.5 to 2 m3/s. The preceding evidence-only batch is 2026-10-23-deferred-fluid-hold.

- Use the archived format-17/unit-10 graph, source beb5c2fc, island 94 committed tick 21577, epoch 27212. Validate copied unit hashes; never open or change the saved world.
- Work in the owned checkout on codex/wti-pumping-rate-hold, based on the runtime source. Leave the committed icon/phase-display follow-ups on codex/fluid-display-followups at a591404f.
- Establish the physical operating branch and reproduce 1.5 versus 2 m3/s with the original algorithms and captured package settings. Anchor conclusions to the rate change; do not assume the separate 200°C generator is the held loop.
- Trace only existing residual/mode/state values in offline source copies. Isolate whether the failure belongs to thermodynamics, hydraulics, cached initialization or interval control. No production instrumentation, new laws or tolerance changes.
- Make one narrow cache-validity counterfactual reviewable as an offline patch. Preserve within-solve phase/mode continuation; keep all physical equations and conservation gates. Check the original stable setting and several intervals including a valve transition.
- Report expected behaviour, what the code did, then the proposal and limits. Archive tooling/logs and update both batch indexes. A production fix, permanent regression gate and broad qualification remain separate from this tracing task.

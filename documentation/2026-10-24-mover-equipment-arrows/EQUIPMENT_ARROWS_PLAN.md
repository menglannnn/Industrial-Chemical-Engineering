# Equipment-side flow arrows

2026-10-24. Owner request: inlet/outlet indicators belong on the equipment, rather than connected piping.

- Branch codex/mover-arrows-on-equipment from b70e92aa in the owned pipeline-tool worktree.
- Paint one word-free arrow band on each constructed pump/compressor suction/discharge nozzle neck.
- Point suction toward the machine and discharge away; support all headings and drawn nozzle sizes.
- Bake marks with equipment static/fixed surfaces, retaining them during running animation.
- Remove neighboring-pipe role metadata and pipe-side decal baking. Keep check-valve casting arrows.
- Owner follow-up: remodel the pump connections so the flange is at the joint and the arrow behind it. Remove redundant built-in pump flanges and extend the sized nozzle neck back to the casting.
- Preserve existing connection planes, model bounds and hydraulic binding cells; retain ordinary nozzle collision generation and fluid formats 18/11.
- Compile/package and use software renders, including connected pipes, all headings, size extremes and bare nozzles. No new tests or automated GUI checks; active client remains open.
- Record review, previews and tool usage locally; no automatic merge/version bump.

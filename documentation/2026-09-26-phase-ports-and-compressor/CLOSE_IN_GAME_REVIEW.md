# The close's in-game check, item C3 (2026-10-14)

Five of six checks pass; check 2 passed its slack stage and its taut and STEADY stage was not observed. No HELD line, no exception from a createcheme class. Four GUI findings (G1-G4) are proposals for the owner; no code was changed. Thread t33 observed, commit `05fce69c`; this review was written from its notes, screenshots and report by thread t34 (t33 stopped at its usage limit before writing it). Decision rows A437 (the check) and A438 (the findings).

## Build and lane

- Code `d2321885` on `claude/phase-ports-compressor`, session worktree, no source edit. Owner's Windows machine, JDK 21.0.11, `JAVA_OPTS=-Xshare:off`, `runMcpClient` with the compat mixins, GUI scale 1, 854x480.
- Lane: `t33-lane.sh alone` held the alone lock for the whole client session (22:31:50-23:03:12, about 31 minutes of the 75-minute box), released with rc=0. No test JVM before the launch.
- Fresh world "Close in-game C3" (Creative, commands on, Superflat, no structures): `fluid_world status=LOADED format=7 online_tick=0 islands=0` at 22:33:15. Game time = wall time minus 22:33:15.
- Every step through the minecraft-mod-mcp bridge over HTTP (skill as of `03952fd3`). No desktop input. Record: `tools/phase-ports-probes/in-game-close/` (`NOTES.md`, screenshots `01`-`06`, `05x`, `cmds/`, `client-latest.log`, `gradle-runMcpClient.log`).
- The merged code `1cce9a2b` adds W1 (the booking of a residue vessel) and W2 (the pass loop's closure at a polished point). None of the six checks has a residue vessel or a polished forbidden direction, so this evidence stands for the merged code.

## Checks

| # | check | result | evidence (game time, screenshot) |
|---|---|---|---|
| 1 | A placed 1000 L tank alone holds nothing | PASS | `EMPTY: holds nothing / STEADY: no flow since 114.1 s`, "No components in this phase." (195.1 s, `01`); "No connection leaves this tank." (215.1 s, `01b`) |
| 2 | A 130 kPa water generator fills the tank through a 0.01 m pipe | PARTIAL: slack stage PASS, taut and STEADY not observed | `Side (west): mixed outlet / receiving · 66.5 kmol/h` (0.333 kg/s); 101.3250 kPa, 25.0 C, 252.3 L, 25.2 %, 251.3 kg (940.6 s, `02a`); 101.3250 kPa, 529.7 L, 53.0 %, 527.6 kg (1770.6 s, `02b`) |
| 3 | A compressor (ratio 3) from a 150 kPa nitrogen generator into an empty tank (A327) | PASS | running: `FULL / PUMP_TARGET (limit 299291 Pa at this suction)`, ratio 2.0 (526.9 s, `03a`); tank `FULL`, 449.9955 kPa, 149.8 C (423 K), 100.0 %, 3.6 kg (541.9 s, `03b`); compressor `STEADY: replaying 3.765e-21 kg/s since 549.7 s ... / CLOSED (limit 300000 Pa at this suction)`, 150.0000 kPa, ratio 3.0 (551.9 s, `03c`) |
| 4 | A compressor between two placed empty tanks | PASS | `STEADY: no flow since 267.9 s / INLET_WRONG_PHASE / ERROR: compressor inlet dry (from reservoir at 0, -57, 30)`, 25.0 C, ratio 1.0 (576.1 s, `04a`); discharge tank `EMPTY: holds nothing / STEADY: no flow since 267.9 s` (271.1 s, `04b`) |
| 5 | A pump (0.001 m3/s) drains a filled tank (twin tank T5, see below) | PASS | T5 102.6 kg, 10.3 % (651.5 s, `05a`); `FULL / PUMP_TARGET (limit 500009 Pa on this fluid)`, 199.0 kmol/h, 121.7125 kPa (913.8 s, `05b`); `STEADY: no flow since 1015.7 s / INLET_WRONG_PHASE / ERROR: pump inlet dry (from reservoir at 0, -57, 50)` (1028.8 s, `05c`); heel 2.1 kg, 0.2 %, 101.3250 kPa (1018.8 s, `05d`) |
| 6 | The compressor between two filled nitrogen tanks (A332) | PASS | suction charged to 250.0000 kPa, 85.7 C, 2.3 kg (532.4 s, `06a`); `STEADY: replaying 4.235e-23 kg/s ... / CLOSED (limit 259842 Pa at this suction)`, ratio 3.0 (673.0 s, `06b`); suction 129.9212 kPa, 25.2 C, 1.5 kg (`06c`); discharge 389.7630 kPa, 133.5 C, 3.2 kg (`06d`), the A332 GameTest's 129921.20 Pa, 389762.95 Pa and 406.67 K |

Check 5 ran on a twin of check 2's line: at 0.333 kg/s check 2's tank would not be full inside the time box. T5 filled the same way to 108.6 kg (t33's report), its generator was removed, and a pump line was placed under it. The 2.1 kg heel is within A334's 0.5 % band, refused per slice (D59).

Check 6's brief expected the suction to stay at the ambient and its temperature. That holds for a slack bag, not for this 250 kPa charge. The suction bag stays taut and the gas left in it expands isentropically: 358.9 K x (129.92 / 250)^0.2857 = 297.6 K; shown 298.35 K. The code is right; the brief needed this correction.

## Findings (proposals, no code changed)

| # | what the view shows | physics expected | proposition |
|---|---|---|---|
| G1 | A pump placed directly on a tank face reads `ERROR: pump needs two ports without a zero-storage bypass`; the tank says "No connection leaves this tank" (778.3 s, `05x`) | Under D20 only pipes connect, so the pump is not joined to the tank. The message is right about the state but does not name the cause | Name the cause: "equipment joins only through pipes" |
| G2 | Check 4's compressor table lists Nitrogen 100 %, 0.0409 kmol, while both tanks are EMPTY (`04a`) | The island holds nothing (A327); 0.0409 kmol is the idle seed at 1 atm, not material | Show "No components" as the EMPTY tanks do |
| G3 | Roundoff flows are printed: `replaying 3.765e-21 kg/s` (`03c`), `4.235e-23 kg/s` (`06b`) | Below the bag's resolution nothing flows (D59 (2)) | Print "no flow" below the bag's resolution |
| G4 | The rail of an EMPTY tank is blank: no fill, no capacity (`01`, `04b`) | An empty tank is 0 % of a known capacity | Show fill 0.0 % and the capacity |

## Not observed, and why

- Check 2's taut and STEADY stage. A 1000 L tank at 0.333 kg/s needs about 53 minutes of client time to fill (full near 3190 s). The thread stopped at 1770.6 s on its usage limit. It is not re-run (orchestrator, D50 lean measurement): check 3 already shows a bag going slack then taut to its generator's pressure with the island certified STEADY (A327), on the same code path.
- The time box was not the limit; about 31 of its 75 minutes were used.

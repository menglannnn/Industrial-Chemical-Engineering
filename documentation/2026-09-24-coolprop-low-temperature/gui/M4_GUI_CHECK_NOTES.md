# M4 GUI check notes (2026-10-16, `61a1eba0`, `runMcpClient` lane, bridge only)

World: fresh copy of `tools/fluid-in-game-rig/template/bench-template` as `run/mcp-client/saves/m4-crystals-20261016/`
with the `p6_pilot` datapack (package `createcheme:pilot_cryogenic`). Build commands, helpers and logs:
`tools/unified-thermo-port-m4/` (`mcp/build-*.txt`, `logs/m4-logs.tar.gz`). Window 854x480, GUI scale 2.

Expectation (physics first): cold N2 (150 K, then 120 K) and CO2 gas (220 K), both at 300 kPa, mix in a 1 m3 tank
vented to 101 kPa. The CO2 partial pressure (about 78 kPa) is above the CO2-I sublimation pressure at the tank's
180 K (about 27 kPa), so dry ice deposits in the tank. The sublimation heat (about 26 kJ/mol) limits it to a few
per cent of the molar feed: about 1.4 mol/s, 0.06 kg/s at 147 kmol/h. The dry ice is one solid at one size.

| Shot | What it shows |
|---|---|
| 00-00c | World list filtered to the M4 world (the P6 world is never opened); log `fluid_world status=LOADED format=9 online_tick=0 islands=0` |
| 01 | First open of a generator replays the last published view ("Last delivered view: 30.1 s"), water at 25 C |
| 02-05b | Generator A: Nitrogen preset, 300000 Pa, -123.15 C, "Applied"; generator B: Carbon dioxide, 300000 Pa, -53.15 C, vapour 100 % |
| 06-07c | Tank after the connections: 182 K, 300 kPa, 88.7 % N2 / 11.3 % CO2, Solids tab headers "Solid / Mean size / Mass (kg)", no solid; rail "No flow"; all faces "no flow" |
| 08, 08a | The side outlet pipe to the void: "blocked with solid / STEADY: no flow since 409.1 s", nothing in its Solids tab (open item 2 of the stage report) |
| 09, 09b | Generator A changed to -153.15 C (120 K): "Applied" |
| 10, 10b | Solids tab: one row "Carbon dioxide (solid" (the name column cuts the parenthesis at this width), 250.0 um, 0.00129 kg |
| 11 | Rail: Vapor 100.0, Solids 8.21e-5 % (the crystal's share), still "No flow" |
| 12 | Mass basis and kelvin: the solid row unchanged |
| 13-15 | Save and reload 1: log `format=9 online_tick=16438 islands=3 certificates_saved=3 certificates_restored=3 awake=0`; before 181.8 K, 300.0003 kPa, 0.00129 kg; after the same |
| 16 | The side outlet after the reload: "blocked with solid: NO_LIQUID_CARRIER / STEADY: no flow since 677.0 s" |
| 17 | A top vent added (pipe and void above the tank): "Top (up): gas outlet, drawing gas 141.5 kmol/h"; west and north "mixed outlet, receiving" 103.9 and 43.1 kmol/h |
| 18, 18b, 19, 20 | Dry ice growing at the top vent: 2.6 kg (1041.1 s), 3.2 kg (1051.1 s), 5.6 kg (1081.1 s), 7.6 kg (1111.1 s), about 0.07 kg/s; rail Mass 12.2 kg, Vapor 99.6 % / Solids 0.4 % (5.6 kg / 1571 kg/m3 = 3.6 L of 1000 L), "Ready" |

The pause before save and reload 2 crashed the integrated server in `FluidWorldAuthority.capture`
(`NoSuchElementException`, crash report in the logs archive): open item 1 of the stage report. The world is left as it
is; it crashes at its next save. Not exercised: the crystal row's hover tooltip (the bridge moves the real cursor for a
hover, which the no-desktop-input rule avoids), a typed hold, a pending event across a reload.

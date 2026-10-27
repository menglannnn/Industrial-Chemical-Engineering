# Sensor and valve GUI check
2026-10-27. One runMcpClient session in the owned worktree, official pinned v0.4.18/background input mode. No desktop keyboard/mouse input was used. No suite ran while this dev client was open.

Expected: housings/ghosts carry no live world-space readings; requested GUI rows replay cached accepted data, subscribe, and stop on close. A bare valve is fully open; a compatible single module selects pressure/flow control. Empty level is valid zero.

Observed through the bridge:
- A bare valve shows “Lacking components” and 100% opening.
- The first live check exposed FluidDeviceBlockEntity.refreshGeometry's old PressureValve-only type check, which cleared a modular valve's fitting. It now recognizes FlowControl.Valve and retains the configured direction.
- Only the two existing method bodies were recompiled and hot-reloaded into this owned client; it was not restarted for another check. The follow-up server GameTest pins both projected and committed refreshes.
- Normal item use installs a valve body and pressure module. A second flow module is rejected without changing its persistent UUID/mount. Shift-wrench removal clears that sensor; a replacement flow module receives a new UUID/revision.
- After setting the source to 300 kPa and enabling it, the forward flow valve regulates the entered .0001 m³/s actual target. The GUI shows about .1 kg/s, .000100 m³/s and .3% opening. Reversing the configured direction correctly closes and reports limited control; the connected stopped flowmeter shows zero kg/s and m³/s.
- A pipeline temperature probe reads 25 °C from the accepted flowing stream.
- The native constructed vertical drum takes a digital body meter. Its empty GUI shows 0 m / 0%; the world housing is blank. Placement ghosts and installed housings are visible.
- Initial missing process data shows “Unavailable” / “Signal unavailable”. Native GUI rail scrolling exposes sensor rows. No live values appear on the physical housings.

The bridge's shortcut command path did not execute these server commands; its real ChatScreen input path was used. The key-hold shortcut did not persist crouch, so a local serviceability helper set Minecraft's own shift mapping while the bridge invoked ordinary use. The raw screenshot helper crops the lower-left 854×480 of a 1280×720 viewport; the inspected rows and housings remain visible. Helpers and native captures are in tools/process-control/; no helper enters the jar.
The client saved its fresh test world and quit. Post-client server verification passed all 24 required tests. Other vessel sizes/headings, boot-pad distinction, shared socket occupancy, recovery/reload and overrange are covered by gates rather than additional client sessions.

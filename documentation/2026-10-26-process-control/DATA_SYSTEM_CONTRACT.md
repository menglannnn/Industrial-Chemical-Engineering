# Later data-system contract

Status: Planned; approved 2026-10-27. No cable blocks or remote level controller ship in the sensor/local-controller batch.

- Endpoint identity: persistent fluid host ID, logical mount, sensor instance UUID, configuration revision. Positions are presentation/placement data; replacing a sensor never silently rebinds its replacement.
- One unbranched cable component joins one sensor to one valve, in one dimension and fluid island. Reject branches, extra endpoints, fan-out and a local-module-plus-cable combination before consumption.
- Registered cable facts survive chunk unloading; queued place/remove events rebuild disposable connectivity. Never load chunks to trace a cable.
- Initially accept body-level or boot-water level sensors. Source readings in UI are accepted publications; solver input is the trial state at that source identity.
- Level target is either metres above the sensor datum, or a fraction of its measuring height. Percentage is height percentage, never volume percentage. Default: 50%, Outlet action.
- Outlet action opens on high level; Inlet action opens on low level. A control row uses geometry.volumeAt(targetHeight) and the coupled end-of-step phase inventory. The boot datum adds bootDepth to the body-relative geometry height.
- One island may contain independent body and boot loops. Extend Jacobian sparsity to source-vessel columns; prevent certification across a pending link/target event.
- Missing signal or an invalid link holds the last accepted effective opening. With no retained opening, use fully open. New topology/configuration retries the binding on the engine schedule.
- Detect dependent control rows before accepting a coupled solve. Report affected loops and solve their fixed restrictions at retained openings; preserve unrelated thermodynamic diagnostics.
- Ideal valve characteristic: nonnegative head = rho*v^2/2*(u^-2-1), u in (0,1], exact zero flow for u=0. No shaft work.
- Rate/startup paths must use attainable stationary balances or opening limits, never a constant transient level-error equation.
- Test metres/height %, both actions, simultaneous independent loops, duplicate/conflicting bindings, disconnection, unload/reload, target changes, limited authority and step-size stability.

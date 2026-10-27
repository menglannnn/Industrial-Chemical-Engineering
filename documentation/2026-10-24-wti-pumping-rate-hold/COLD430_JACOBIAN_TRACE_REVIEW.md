# Cold 430 Jacobian-sign trace and validation

2026-10-24; branch `kimi/remaining-holds-physics-review`; baseline `cb89f23a`; candidate solver SHA-256 `39b0794a869bfd136f57687479b8e4f855c60c679d30d0124b5bde8e155e6f47`.
Outcome: the one-line solver repair and strengthened cold-430 gate are retained for owner review, uncommitted and unmerged. The combined worktree is not merge-ready because the separate, paused cold-431 guard still fails a slow gate.

## 1. Expected physical behaviour

At tick 404, CO2-rich gas should expand into the empty tank and LNG should flow only across a favourable pressure difference. Choking and one-way closure are valid regimes; the conserved filling transient should continue without a numerical hold.
With other end pressures fixed, a connection's contribution to a junction's net inflow should be non-increasing in that junction's own pressure. Cap-flat and refused segments have zero slope. The balance residual therefore needs a non-positive diagonal: negative curvature gives a descent model and zero is regularized, but roundoff must not reverse that sign.

## 2. What the code did instead

In the first mint solve, iteration 1, the raw diagonals were `[-6.595681167e-7, +1.089327902e-14, -2.882660634e-5]`; the floor was only `2.882660634e-17`. The positive entry came from edge 6's roughly two-ulp flow difference while all three connections at that junction were cap-flat.
That junction needs to fall below about `0.8426 MPa` to receive inflow. Instead Newton alternated it around `2.4958` and `4.89867 MPa`. Tiny Armijo improvements prevented every sweep; 30 iterations ended at residual `5.501074679 kg/s` against tolerance `1.338666569e-5 kg/s`, followed by E6 at tick 406. The existing absolute-value floor did not replace a positive diagonal larger than that floor.

## 3. Proposal and validation

Implemented candidate: when `jacobian[i][band] >= 0`, use the existing `-floor`, retaining the previous small-magnitude rule. This adds no fallback, flow law, tolerance, iteration budget or conservation change. The cold-430 gate now requires `w.held.isEmpty()` through tick 504 as well as no capability hold.
Owner validation is still required for the global monotonicity assumption where PH density/cap tables and mover head vary with pressure. If a physically meaningful positive diagonal exists, this unconditional projection is invalid and a narrower roundoff rule is needed; the matched worlds did not establish that counterexample.

- Focused candidate gate: pass. Disabled-rule negative control: expected E6 assertion failure at tick 406.
- Matched seeds 1-200, all distributions: no newly held seeds, changed first-hold classes or increased hold publications. Newton-iteration deltas were cold `+3418` (about 0.66%), bundled-random `-73`, defaults `0`; these are counters, not timing claims.
- One selected-candidate 1000-seed campaign, all distributions, eight workers, alone: clean counts were cold `975`, bundled-random `993`, defaults `1000`. Cold 430 recovered from 9 preserved-baseline holds to 0. Totals alone were not used as regression evidence.
- Historical full comparisons remain watchlist evidence because that baseline predates current scope and the paused solid guard. Isolated seed 244 was clean in both arms; seed 855 had the same nine deadline publications in both. Matched cold 743 produced the same five E6 publications and residuals in both arms. Other historical class/publication deltas are not claimed as candidate attribution or regression freedom.
- Quick suite: 1369 tests, four established unrelated failures and no additional failure. Slow gates: 185 tests, the established nozzle failure plus cold 743. Candidate solver with the original solid class passed that cold-743 gate; baseline solver with the current solid guard failed it identically. The extra gate failure therefore belongs to the paused cold-431 work, not this candidate, but still blocks the combined branch.

Do not merge the combined worktree. First resolve the paused solid-guard failure and obtain owner approval for the monotonicity invariant; the solver change can then be isolated from the unrelated files for merge qualification.
Evidence and reproduction: `tools/wti-pumping-rate-hold/cold430-jacobian-trace/`; compressed logs: `tools/wti-pumping-rate-hold/logs/cold430-jacobian-trace-logs-2026-10-24.tar.gz`. No GameTest, client check, commit, version/changelog change or code-guide change was made.

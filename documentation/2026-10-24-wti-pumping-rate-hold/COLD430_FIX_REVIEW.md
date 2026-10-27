# Cold 430 targeted-balance validation

2026-10-24; branch `kimi/remaining-holds-physics-review`; baseline `cb89f23a`. Three restricted Newton/bisection forms were tested. **All three are rejected; both edited Java files were restored and no fix is retained.** Evidence: `tools/wti-pumping-rate-hold/cold430-validation/`.

## 1. Expected physical behaviour

At tick 404, CO2-rich gas (4.890 MPa, 293 K) should expand into the empty tank while LNG (0.884 MPa, 128 K) supplies only across a favourable pressure difference. The seven traced connections are passive. Choking, zero reverse flow and changing phase fractions are valid regimes; the conserved filling/mixing transient should continue without a capability or numerical hold.

## 2. What the code and candidates did

On the baseline, the existing eight-block gate rejects capability holds but permits E6 at tick 406. The preceding mint balance accepts negligible Newton descents on artificial-curvature rows, stalls at residual 5.501074679 kg/s against tolerance 1.338666569e-5 kg/s, and seeds the later failed step. The trace does not establish missing thermodynamics.

### Form 1: targeted bisection on any strict merit improvement

The intended regularized-row trigger fired at iteration 16; merit fell from 15.2004063835 to 13.2128837463. The strengthened gate passed through tick 504; disabling only the fallback restored the expected tick-406 failure.
The 200-seed comparison had no new holds, class changes or increased publications in any distribution. The one selected-form full campaign finished clean at 976/1000 cold, 994/1000 bundled-random and 1000/1000 defaults, but these totals do not establish regression freedom.
Exact-current cold coverage exposed seed 555: eight new E5 publications, first at tick 2464, island 53, committed 2462, residual 229.463. Its copied-solver trace records an iteration-3 targeted update from merit 111478.83189746414 to 111474.97124012474 (about 3.46e-5 relative gain) before that E5 trajectory. Cold 431 also increased from 7 to 11 publications; cold 475 acquired wall-deadline records.

### Form 2: require the existing full-step Armijo decrease

This guard rejects cold 555's tiny gain while retaining cold 430's roughly 13% gain. The focused gate and disabled-rule negative control behaved as intended; cold 555 completed clean in its diagnostic replay. No second full campaign was run.
Matched seeds 1-200 had no changed classification or increased publication in any distribution. Seeds 401-600 recovered cold 430, but cold 431 increased 7 -> 11 and cold 475 increased 0 -> 3; bundled distributions were unchanged. Quick-check Newton iterations (baseline -> candidate) were cold 631130 -> 631230, bundled-random 150734 -> 149591 and defaults 13103 -> 13115; no elapsed-time conclusion is drawn.
Complete nonverbose, counter-free, one-worker replays removed the earlier verbose-harness truncation. Cold 431 retained the 7 -> 11 increase: five versus nine wall-deadline publications, with final island 42 `FULL` on baseline but `HELD: wall deadline` on the candidate. Cold 475 completed clean in both forms, so its campaign-only records are not the rejection basis.

### Form 3: evaluate both steps and retain the lower accepted merit

The focused cold-430 gate failed immediately at tick 406 with E6 residual 2.6004703419428292. No broader comparison was run.

## 3. Proposal for owner validation

Do not merge any of these forms. A local merit decrease changes which numerical model supplies the next conserved state; it is not sufficient evidence of a physically better trajectory. Keep the original no-capability-hold test unchanged until a candidate can also demonstrate keep-up without the observed regressions.
Next, trace cold 431's island 42 at committed tick 2158: pressures, phases, flows, active modes and accepted/rejected steps should explain the extra deadline attempts before another numerical form is proposed. This is an investigation proposal, not an approved solver change.
Form 1's repository runs had 1368 quick tests/four failures and 185 slow tests/one failure; all five are the established unrelated baseline failures, with no additional unit failure. Form 2 was rejected before another suite or campaign. The restored baseline `compileJava` succeeds.
No commit, merge, version/changelog change, GameTest or client check was made. Tooling cleanup: all probes, patches, harnesses and reports are under `cold430-validation/`; logs are in `logs/cold430-validation-logs-2026-10-24.tar.gz`. Code guide: no change.

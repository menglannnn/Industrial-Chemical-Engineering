# trace-flush

Batch `documentation/2026-09-26-phase-ports-and-compressor`. Logs of the trace flush (owner decision D16, commit `f80def43`) and of the restated F7 tests. The code and its gate tests are tracked (`PassiveIntervalSolver.flushTraces`, `TraceFlushLedgerTest`, `TraceFlushLiquidFullTankTest`, `TraceFlushFedPumpTest`). Nothing here was detached from a tracked path.

| File | Content |
|---|---|
| `logs/01-new-tests.log` | the three new test classes and `IslandCertificateTest` on the flush, before the full gates |
| `../hold-classes/logs/t01-o2-t1-*` | the batch gates on `f80def43` (`../hold-classes/gates.sh logs/t01-o2-t1 trace-flush-20260927`) |
| `../hold-classes/logs/t02-campaign-f80def43-*` | the 1000-seed fuzz campaign in both distributions (`../hold-classes/campaign.sh logs/t02-campaign-f80def43`) |
| `../holds2/logs/20`-`22` | hold F7 at the 900 kg/m3 reference: the four classes before and after the tests were restated |

The prototype this started from is `../holds/src/option-t1-trace-flush.patch` (unbooked).

package com.wormzjl.createcheme.runtime.fluid;

import org.junit.jupiter.api.Test;

/**
 * Hold class F of the placement-sequence fuzz (PHASE_PORTS_REVIEW.md, "Hold search by placement-sequence fuzz
 * (2026-09-27)"), smallest seed 39 of the 1000-seed campaign on 65583a38 (logs/08). Budget hold: the full solve exceeds the 2 s wall budget (or the soft budget's approximate fallback is refused) at every retry, deterministically.
 * Fails on 65583a38. Run: the hold-fuzz init script, --tests '*HoldClassFBudgetReproTest'.
 */
class HoldClassFBudgetReproTest {
    @Test void seed39RunsWithoutAHold(){HoldFuzzTest.assertSeedRunsClean(39);}
}

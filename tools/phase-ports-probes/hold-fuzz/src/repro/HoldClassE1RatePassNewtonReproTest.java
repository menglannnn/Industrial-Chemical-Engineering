package com.wormzjl.createcheme.runtime.fluid;

import org.junit.jupiter.api.Test;

/**
 * Hold class E1 of the placement-sequence fuzz (PHASE_PORTS_REVIEW.md, "Hold search by placement-sequence fuzz
 * (2026-09-27)"), smallest seed 6 of the 1000-seed campaign on 65583a38 (logs/08). The interval-start rate pass fails to converge (Newton iteration limit or line-search stall at pass 0) and, unlike the interval solver's own port-rate check, its failure is not tolerated, so the island holds.
 * Fails on 65583a38. Run: the hold-fuzz init script, --tests '*HoldClassE1RatePassNewtonReproTest'.
 */
class HoldClassE1RatePassNewtonReproTest {
    @Test void seed6RunsWithoutAHold(){HoldFuzzTest.assertSeedRunsClean(6);}
}

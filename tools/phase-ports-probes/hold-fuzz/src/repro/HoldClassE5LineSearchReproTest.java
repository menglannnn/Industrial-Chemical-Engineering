package com.wormzjl.createcheme.runtime.fluid;

import org.junit.jupiter.api.Test;

/**
 * Hold class E5 of the placement-sequence fuzz (PHASE_PORTS_REVIEW.md, "Hold search by placement-sequence fuzz
 * (2026-09-27)"), smallest seed 48 of the 1000-seed campaign on 65583a38 (logs/08). The step Newton's line search stalls at pass 0 at every step size (residual 0.27): a two-phase crude generator with a pump and pressure valves into a tank.
 * Fails on 65583a38. Run: the hold-fuzz init script, --tests '*HoldClassE5LineSearchReproTest'.
 */
class HoldClassE5LineSearchReproTest {
    @Test void seed48RunsWithoutAHold(){HoldFuzzTest.assertSeedRunsClean(48);}
}

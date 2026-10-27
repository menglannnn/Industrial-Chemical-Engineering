package com.wormzjl.createcheme.runtime.fluid;

import org.junit.jupiter.api.Test;

/**
 * Hold class E6 of the placement-sequence fuzz (PHASE_PORTS_REVIEW.md, "Hold search by placement-sequence fuzz
 * (2026-09-27)"), smallest seed 33 of the 1000-seed campaign on 65583a38 (logs/08). The step Newton stops at its iteration limit a little above tolerance (3.4e-7) at every step size: a pure-water network (one generator, two voids, loops of 0.01-0.05 m pipes).
 * Fails on 65583a38. Run: the hold-fuzz init script, --tests '*HoldClassE6NewtonLimitReproTest'.
 */
class HoldClassE6NewtonLimitReproTest {
    @Test void seed33RunsWithoutAHold(){HoldFuzzTest.assertSeedRunsClean(33);}
}

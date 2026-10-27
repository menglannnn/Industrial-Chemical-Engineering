package com.wormzjl.icheme.runtime.fluid;

import org.junit.jupiter.api.Test;

/**
 * Hold class E2 of the placement-sequence fuzz (PHASE_PORTS_REVIEW.md, "Hold search by placement-sequence fuzz
 * (2026-09-27)"), smallest seed 4 of the 1000-seed campaign on 65583a38 (logs/08). Every backward-Euler step is singular (after passes 0-5) at every step size: a near-null direction in the liquid amounts of a two-phase crude junction (phase split undetermined).
 * Fails on 65583a38. Run: the hold-fuzz init script, --tests '*HoldClassE2StepSingularReproTest'.
 */
class HoldClassE2StepSingularReproTest {
    @Test void seed4RunsWithoutAHold(){HoldFuzzTest.assertSeedRunsClean(4);}
}

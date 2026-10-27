package com.wormzjl.icheme.runtime.fluid;

import org.junit.jupiter.api.Test;

/**
 * Hold class E10 of the placement-sequence fuzz (PHASE_PORTS_REVIEW.md, "Hold search by placement-sequence fuzz
 * (2026-09-27)"), smallest seed 333 of the 1000-seed campaign on 65583a38 (logs/08). No finite Jacobian trial at the first step after a placement: a column cannot be differenced inside the property domain from the minted start (a crude generator at 376 K into junctions minted with 268 K nitrogen).
 * Fails on 65583a38. Run: the hold-fuzz init script, --tests '*HoldClassE10NoFiniteJacobianReproTest'.
 */
class HoldClassE10NoFiniteJacobianReproTest {
    @Test void seed333RunsWithoutAHold(){HoldFuzzTest.assertSeedRunsClean(333);}
}

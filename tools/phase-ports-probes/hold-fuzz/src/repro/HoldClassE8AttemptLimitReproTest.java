package com.wormzjl.createcheme.runtime.fluid;

import org.junit.jupiter.api.Test;

/**
 * Hold class E8 of the placement-sequence fuzz (PHASE_PORTS_REVIEW.md, "Hold search by placement-sequence fuzz
 * (2026-09-27)"), smallest seed 613 of the 1000-seed campaign on 65583a38 (logs/08). The interval's 1024 attempts run out on Newton failures at steps of milliseconds (line search stalled at 1.3e-9 against a 1e-9 tolerance, 483 times) in water junctions holding trace vapour.
 * Fails on 65583a38. Run: the hold-fuzz init script, --tests '*HoldClassE8AttemptLimitReproTest'.
 */
class HoldClassE8AttemptLimitReproTest {
    @Test void seed613RunsWithoutAHold(){HoldFuzzTest.assertSeedRunsClean(613);}
}

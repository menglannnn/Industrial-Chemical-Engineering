package com.wormzjl.icheme.runtime.fluid;

import org.junit.jupiter.api.Test;

/**
 * Hold class G of the placement-sequence fuzz (PHASE_PORTS_REVIEW.md, "Hold search by placement-sequence fuzz
 * (2026-09-27)"), smallest seed 236 of the 1000-seed campaign on 65583a38 (logs/08). Singular matrix: a component row of a two-phase crude junction (39 unknowns) has no entry at the interval-start rate pass.
 * Fails on 65583a38. Run: the hold-fuzz init script, --tests '*HoldClassGZeroRowReproTest'.
 */
class HoldClassGZeroRowReproTest {
    @Test void seed236RunsWithoutAHold(){HoldFuzzTest.assertSeedRunsClean(236);}
}

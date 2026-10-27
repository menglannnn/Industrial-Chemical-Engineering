package com.wormzjl.icheme.runtime.fluid;

import org.junit.jupiter.api.Test;

/**
 * Hold class E3 of the placement-sequence fuzz (PHASE_PORTS_REVIEW.md, "Hold search by placement-sequence fuzz
 * (2026-09-27)"), smallest seed 30 of the 1000-seed campaign on 65583a38 (logs/08). Hydrocarbon vapour partial pressure below the numerical floor at every step size: nitrogen entering a junction minted from a water generator (pure water), in a four-node island of a water generator, a nitrogen generator and a void.
 * Fails on 65583a38. Run: the hold-fuzz init script, --tests '*HoldClassE3VapourFloorReproTest'.
 */
class HoldClassE3VapourFloorReproTest {
    @Test void seed30RunsWithoutAHold(){HoldFuzzTest.assertSeedRunsClean(30);}
}

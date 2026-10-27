package com.wormzjl.icheme.runtime.fluid;

import org.junit.jupiter.api.Test;

/**
 * Hold class D of the placement-sequence fuzz (PHASE_PORTS_REVIEW.md, "Hold search by placement-sequence fuzz
 * (2026-09-27)"), smallest seed 118 of the 1000-seed campaign on 65583a38 (logs/08). The interval-start rate pass (SolidEventIntegrator, any island with a filter) is structurally singular: every connection from the island's junctions to a pressure anchor starts on the velocity cap's branch, which has no pressure column, so the junctions' pressure level has no row.
 * Fails on 65583a38. Run: the hold-fuzz init script, --tests '*HoldClassDSingularRatePassReproTest'.
 */
class HoldClassDSingularRatePassReproTest {
    @Test void seed118RunsWithoutAHold(){HoldFuzzTest.assertSeedRunsClean(118);}
}

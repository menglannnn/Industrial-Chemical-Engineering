package com.wormzjl.createcheme.runtime.fluid;

import org.junit.jupiter.api.Test;

/**
 * Hold class E4 of the placement-sequence fuzz (PHASE_PORTS_REVIEW.md, "Hold search by placement-sequence fuzz
 * (2026-09-27)"), smallest seed 168 of the 1000-seed campaign on 65583a38 (logs/08). The conservative reconstruction fails the equation gate after singular and stalled passes, in water-filled junctions holding vapour shares of 1e-233 to 1e-278 (the vanishing trace-vapour family of F1/F2).
 * Fails on 65583a38. Run: the hold-fuzz init script, --tests '*HoldClassE4EquationGateReproTest'.
 */
class HoldClassE4EquationGateReproTest {
    @Test void seed168RunsWithoutAHold(){HoldFuzzTest.assertSeedRunsClean(168);}
}

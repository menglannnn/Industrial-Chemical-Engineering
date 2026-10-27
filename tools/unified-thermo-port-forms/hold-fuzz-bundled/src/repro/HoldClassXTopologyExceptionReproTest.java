package com.wormzjl.icheme.runtime.fluid;

import org.junit.jupiter.api.Test;

/**
 * Hold class X of the placement-sequence fuzz (PHASE_PORTS_REVIEW.md, "Hold search by placement-sequence fuzz
 * (2026-09-27)"), smallest seed 28 of the 1000-seed campaign on 65583a38 (logs/08). Not a hold: IllegalStateException 'Topology event reached boundary 1 outside the islands it fenced' from PhysicalRegistry.apply. An event placed next to a device a queued event places (the walk in submit stops there) is not fenced on the island beyond it when the queued device ends up unowned (a dead-end pipe), and the batch's component walk reaches that island's boundary or filter.
 * Fails on 65583a38. Run: the hold-fuzz init script, --tests '*HoldClassXTopologyExceptionReproTest'.
 */
class HoldClassXTopologyExceptionReproTest {
    @Test void seed28RunsWithoutAHold(){HoldFuzzTest.assertSeedRunsClean(28);}
}

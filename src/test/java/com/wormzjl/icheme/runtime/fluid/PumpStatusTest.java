package com.wormzjl.icheme.runtime.fluid;

import com.wormzjl.icheme.science.fluid.network.FlowControl;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PumpStatusTest {
    private final FlowControl.Pump pump=new FlowControl.Pump(2,500000);

    @Test void operatingLimitsAndRefusalsAreDeviceErrorsOnAnAdvancingIsland() {
        for(var mode:new FlowControl.Mode[]{FlowControl.Mode.PUMP_HEAD_LIMIT,FlowControl.Mode.PUMP_SUCTION_LIMIT,FlowControl.Mode.INLET_WRONG_PHASE,FlowControl.Mode.CLOSED}) {
            String status=PumpStatus.of("FULL",pump,mode,"pump inlet is not liquid (from generator)");
            assertTrue(status.startsWith("ERROR: pump"));assertTrue(status.endsWith(" / FULL"));assertFalse(status.contains("HELD"));
        }
        assertTrue(PumpStatus.of("FULL",pump,FlowControl.Mode.PUMP_HEAD_LIMIT,"").contains("requested flow unavailable"));
        assertTrue(PumpStatus.of("FULL",pump,FlowControl.Mode.INLET_WRONG_PHASE,"pump inlet is not liquid (from generator)").contains("from generator"));
    }

    @Test void offIsNormalAndPresentationDoesNotHideARealSolverFailure() {
        assertFalse(PumpStatus.of("FULL",pump,FlowControl.Mode.PUMP_TARGET,"").contains("ERROR:"));
        assertFalse(PumpStatus.of("FULL",new FlowControl.Pump(2,500000,false),FlowControl.Mode.CLOSED,"").contains("ERROR:"));
        assertTrue(PumpStatus.of("HELD: numerical failure",pump,FlowControl.Mode.PUMP_HEAD_LIMIT,"").contains("HELD: numerical failure"));
    }
}

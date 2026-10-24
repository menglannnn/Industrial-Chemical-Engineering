package com.wormzjl.icheme.runtime.fluid;

import com.wormzjl.icheme.science.fluid.network.FlowControl;

/** A pump's accepted operating condition, presented without changing its island's solver status. */
final class PumpStatus {
    private PumpStatus(){}

    static String of(String islandStatus,FlowControl.Pump pump,FlowControl.Mode mode,String inletReason) {
        String condition=switch(mode) {
            case PUMP_TARGET->"pump running";
            case PUMP_HEAD_LIMIT->"ERROR: pump pressure rise limit reached; requested flow unavailable";
            case PUMP_SUCTION_LIMIT->"ERROR: pump cavitating: suction pressure limit reached";
            case INLET_WRONG_PHASE->"ERROR: "+(inletReason.isBlank()?"pump inlet is dry or has the wrong phase":inletReason);
            case CLOSED->pump.targetVolumeFlow()==0?"pump off":"ERROR: pump cannot drive forward flow";
            default->throw new IllegalArgumentException("Invalid pump operating mode: "+mode);
        };
        return condition.startsWith("ERROR:")?condition+" / "+islandStatus:islandStatus+" / "+condition;
    }
}

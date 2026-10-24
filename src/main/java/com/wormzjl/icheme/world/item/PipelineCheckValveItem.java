package com.wormzjl.icheme.world.item;

import com.wormzjl.icheme.runtime.fluid.PhysicalFluidTopology.Cutoff;

public final class PipelineCheckValveItem extends PipelineValveItem {
    public PipelineCheckValveItem(Properties properties){super(properties,Cutoff.CHECK);}
}

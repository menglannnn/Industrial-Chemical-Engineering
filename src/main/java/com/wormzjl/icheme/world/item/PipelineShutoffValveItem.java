package com.wormzjl.icheme.world.item;

import com.wormzjl.icheme.runtime.fluid.PhysicalFluidTopology.Cutoff;

public final class PipelineShutoffValveItem extends PipelineValveItem {
    public PipelineShutoffValveItem(Properties properties){super(properties,Cutoff.OPEN);}
}

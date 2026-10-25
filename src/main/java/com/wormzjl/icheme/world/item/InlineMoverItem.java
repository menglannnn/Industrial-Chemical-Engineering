package com.wormzjl.icheme.world.item;

import com.wormzjl.icheme.science.fluid.topology.TopologyCompiler;

/** Applies a pump or compressor to an existing horizontal straight pipe. */
public final class InlineMoverItem extends PipelineFittingItem {
    public InlineMoverItem(Properties properties,TopologyCompiler.Kind kind){super(properties,switch(kind){case PUMP->Type.PUMP;case COMPRESSOR->Type.COMPRESSOR;default->throw new IllegalArgumentException("Not an inline mover");});}
}

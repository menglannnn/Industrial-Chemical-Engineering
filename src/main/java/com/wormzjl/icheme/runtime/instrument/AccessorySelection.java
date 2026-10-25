package com.wormzjl.icheme.runtime.instrument;

import net.minecraft.world.phys.AABB;

/** Shared selectable extent for instrument sockets and existing model-based pipe accessories. */
public interface AccessorySelection {
    String id();
    AABB bounds();
}

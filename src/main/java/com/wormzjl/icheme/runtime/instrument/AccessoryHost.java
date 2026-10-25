package com.wormzjl.icheme.runtime.instrument;

/** Static metadata only. Live readings use the requested instruments protocol. */
public interface AccessoryHost {
    InstrumentMetadata instruments();
}

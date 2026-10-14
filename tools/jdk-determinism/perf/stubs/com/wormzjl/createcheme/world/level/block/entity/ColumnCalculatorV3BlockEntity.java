package com.wormzjl.createcheme.world.level.block.entity;

import com.wormzjl.createcheme.science.column.v3.V3ColumnInput;
import java.util.Objects;

/**
 * perf/ stand-in (overrides the cloud-science-harness one in wbuild.sh): the harness stand-in plus the literature
 * preset accessor and package id the column tests read, copied from the real block entity.
 */
public final class ColumnCalculatorV3BlockEntity {
    private ColumnCalculatorV3BlockEntity() {}

    public static final String LITERATURE_PACKAGE = "createcheme:tjl19_dwsim";

    public static V3ColumnInput literatureCduInput() {
        var catalog=com.wormzjl.createcheme.science.material.MaterialRuntime.current();
        return catalog.presets().column("literature_tia_juana").input(catalog);
    }

    public static V3ColumnInput methaneCduInput() {
        var catalog=com.wormzjl.createcheme.science.material.MaterialRuntime.current();
        return catalog.presets().column("tia_juana").input(catalog);
    }

    // read by V3MethaneColumnTest through reflection
    private static V3ColumnInput freshInput() {
        var catalog=com.wormzjl.createcheme.science.material.MaterialRuntime.current();
        return catalog.presets().column(catalog.presets().network().defaultColumnPreset()).input(catalog);
    }

    public record V3Operation(long operationId, long inputRevision, V3ColumnInput input) {
        public V3Operation {
            if (operationId <= 0L || inputRevision <= 0L) {
                throw new IllegalArgumentException("V3 operation identity must be positive");
            }
            input = Objects.requireNonNull(input, "input");
        }
    }
}

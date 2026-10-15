package com.wormzjl.createcheme.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessSolveServicesRequestTest {
    @Test
    void commandCapturesTheCatalogAtAdmissionRatherThanWorkerStart() {
        var original=com.wormzjl.createcheme.science.material.MaterialCatalog.bundled();
        var replacement=com.wormzjl.createcheme.science.material.MaterialCatalog.parse(original.resources());
        try {
            com.wormzjl.createcheme.science.material.MaterialRuntime.publish(original);
            var input=com.wormzjl.createcheme.science.column.v3.V3TestInputs.methaneCduInput();
            var command=command(input);
            com.wormzjl.createcheme.science.material.MaterialRuntime.publish(replacement);
            org.junit.jupiter.api.Assertions.assertSame(original,command.catalog());
            org.junit.jupiter.api.Assertions.assertSame(replacement,command(input).catalog());
        } finally {com.wormzjl.createcheme.science.material.MaterialRuntime.reset();}
    }
    /** The command as the admission path builds it: the catalog is read from the runtime at construction. */
    private static ProcessSolveServices.V3ColumnCommand command(com.wormzjl.createcheme.science.column.v3.V3ColumnInput input) {
        return new ProcessSolveServices.V3ColumnCommand(input,0,0,
                com.wormzjl.createcheme.science.column.v3.V3InitializationOptions.CURRENT,
                com.wormzjl.createcheme.science.column.v3.V3NeuralInitializer.UNAVAILABLE,
                com.wormzjl.createcheme.science.column.v3.V3ColumnCalculator.DEFAULT_LIQUID_SUPPLY_SCREEN_RATIO,
                com.wormzjl.createcheme.science.material.MaterialRuntime.current());
    }
    @Test
    void processWideRequestIdsArePositiveAndStrictlyMonotonic() {
        long first = ProcessSolveServices.nextRequestId();
        long second = ProcessSolveServices.nextRequestId();

        assertTrue(first > 0L);
        assertTrue(second > first);
    }
}

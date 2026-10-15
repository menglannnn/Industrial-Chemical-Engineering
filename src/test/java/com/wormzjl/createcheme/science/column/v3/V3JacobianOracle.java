package com.wormzjl.createcheme.science.column.v3;

import java.util.Objects;

/**
 * Test-tree verification oracle: the dense and the forced stage-colored whole-system finite-difference Jacobian,
 * and the stage blocks extracted from the dense one with an off-band guard.
 */
final class V3JacobianOracle {
    private static final double OFF_BAND_TOLERANCE = 1.0e-10;

    private V3JacobianOracle() {}

    static V3FiniteDifferenceJacobian.Jacobian evaluate(
            V3MeshResidualEvaluator evaluator, V3DryMeshCoordinateMap coordinates, V3DryMeshState state,
            V3FiniteDifferenceJacobian.V3ThermoWorkspaceFactory workspaceFactory) {
        return evaluate(evaluator, coordinates, state, workspaceFactory, V3FiniteDifferenceJacobian.DifferenceScale.FINE);
    }

    static V3FiniteDifferenceJacobian.Jacobian evaluate(
            V3MeshResidualEvaluator evaluator, V3DryMeshCoordinateMap coordinates, V3DryMeshState state,
            V3FiniteDifferenceJacobian.V3ThermoWorkspaceFactory workspaceFactory,
            V3FiniteDifferenceJacobian.DifferenceScale differenceScale) {
        return V3FiniteDifferenceJacobian.evaluate(evaluator, coordinates, state, workspaceFactory, differenceScale,
                V3SolveControl.UNBOUNDED, false, false);
    }

    /** Deterministic qualifier for the stage-colored high-dimensional path, below its size threshold. */
    static V3FiniteDifferenceJacobian.Jacobian evaluateStageColored(
            V3MeshResidualEvaluator evaluator, V3DryMeshCoordinateMap coordinates, V3DryMeshState state,
            V3FiniteDifferenceJacobian.V3ThermoWorkspaceFactory workspaceFactory,
            V3FiniteDifferenceJacobian.DifferenceScale differenceScale) {
        return V3FiniteDifferenceJacobian.evaluate(evaluator, coordinates, state, workspaceFactory, differenceScale,
                V3SolveControl.UNBOUNDED, false, true);
    }

    /** Stored entries, excluding object headers and the small row-offset array. */
    static long storedValueCount(V3FiniteDifferenceJacobian.Jacobian jacobian) {
        long count = 0;
        for (int row = 0; row < jacobian.equations().size(); row++)
            count += jacobian.storedColumnEnd(row) - jacobian.firstStoredColumn(row);
        return count;
    }

    /** Extracts stage-local lower/diagonal/upper blocks from the dense finite-difference Jacobian. */
    static V3BlockJacobian assemble(
            V3ColumnProblem problem, V3MeshResidualEvaluator evaluator, V3DryMeshCoordinateMap coordinates,
            V3DryMeshState state, V3FiniteDifferenceJacobian.V3ThermoWorkspaceFactory workspaceFactory) {
        V3StageBlockLayout layout = new V3StageBlockLayout(Objects.requireNonNull(problem, "problem"));
        V3FiniteDifferenceJacobian.Jacobian full = evaluate(
                Objects.requireNonNull(evaluator, "evaluator"), Objects.requireNonNull(coordinates, "coordinates"),
                Objects.requireNonNull(state, "state"), Objects.requireNonNull(workspaceFactory, "workspaceFactory"));
        double[][] values = full.values();
        double[][][] lower = new double[layout.nodeCount()][][];
        double[][][] diagonal = new double[layout.nodeCount()][][];
        double[][][] upper = new double[layout.nodeCount()][][];
        double maximumOffBandMagnitude = 0.0;
        for (int rowNode = 0; rowNode < layout.nodeCount(); rowNode++) {
            lower[rowNode] = block(values, layout, rowNode, rowNode - 1);
            diagonal[rowNode] = block(values, layout, rowNode, rowNode);
            upper[rowNode] = block(values, layout, rowNode, rowNode + 1);
            for (int columnNode = 0; columnNode < layout.nodeCount(); columnNode++) {
                if (Math.abs(columnNode - rowNode) <= 1) continue;
                maximumOffBandMagnitude = Math.max(maximumOffBandMagnitude, maximumAbsolute(block(values, layout, rowNode, columnNode)));
            }
        }
        if (maximumOffBandMagnitude > OFF_BAND_TOLERANCE) {
            throw new IllegalStateException("V3 MESH Jacobian contains an unexpected off-band coupling of "
                    + maximumOffBandMagnitude);
        }
        return V3BlockJacobian.fromOwnedBlocks(layout, lower, diagonal, upper, maximumOffBandMagnitude);
    }

    private static double[][] block(double[][] values, V3StageBlockLayout layout, int rowNode, int columnNode) {
        int rows = layout.size(rowNode);
        if (columnNode < 0 || columnNode >= layout.nodeCount()) return new double[rows][0];
        int columns = layout.size(columnNode);
        double[][] block = new double[rows][columns];
        for (int row = 0; row < rows; row++) {
            System.arraycopy(values[layout.start(rowNode) + row], layout.start(columnNode), block[row], 0, columns);
        }
        return block;
    }

    private static double maximumAbsolute(double[][] values) {
        double maximum = 0.0;
        for (double[] row : values) for (double value : row) maximum = Math.max(maximum, Math.abs(value));
        return maximum;
    }
}

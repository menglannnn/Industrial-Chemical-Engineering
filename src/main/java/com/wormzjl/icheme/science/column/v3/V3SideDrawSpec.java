package com.wormzjl.icheme.science.column.v3;

/** Specified liquid product rate on a one-based equilibrium tray (not a boundary node). */
public record V3SideDrawSpec(int trayNumber, double molarFlowMolPerSecond) {
    public V3SideDrawSpec {
        if (trayNumber < 1 || !Double.isFinite(molarFlowMolPerSecond) || molarFlowMolPerSecond <= 0.0) {
            throw new IllegalArgumentException("V3 side draws require a positive tray number and finite positive rate");
        }
    }

    // Shared side-draw arithmetic; callers decide whether an invalid split is fatal or diagnostic evidence.

    static Withdrawal withdrawal(V3DryMeshState state, int node, double rateMolPerSecond) {
        if (!Double.isFinite(rateMolPerSecond) || rateMolPerSecond < 0.0) {
            throw new IllegalArgumentException("V3 side-draw rate must be finite and nonnegative");
        }
        double total = liquidTotal(state, node);
        if (!(total > 0.0) || !Double.isFinite(total)) {
            throw new IllegalArgumentException("V3 side draw on tray " + (node + 1) + " has no finite positive liquid flow");
        }
        double fraction = rateMolPerSecond / total;
        if (!Double.isFinite(fraction)) {
            throw new IllegalArgumentException("V3 side draw on tray " + (node + 1) + " has a nonfinite withdrawal fraction");
        }
        return new Withdrawal(total, fraction);
    }

    static double liquidTotal(V3DryMeshState state, int node) {
        double total = 0.0;
        for (int component = 0; component < state.componentCount(); component++) {
            total += state.liquidFlow(node, component);
        }
        return total;
    }

    record Withdrawal(double liquidTotalMolPerSecond, double fraction) {}
}

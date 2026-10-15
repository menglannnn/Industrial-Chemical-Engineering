package com.wormzjl.createcheme.science.column.v3;

import java.util.List;
import java.util.Objects;

/**
 * One prescribed internal heat duty authored as a pumparound between two one-based equilibrium trays.
 *
 * <p>Only the heat effect is modelled: liquid is withdrawn on {@code drawTray}, cooled, and returned on
 * {@code returnTray}, but no circulating stream is added to the internal traffic. {@code dutyWatts} uses the
 * existing {@link V3ColumnSpecification.ReboilerDuty} sign convention, so a positive value adds heat to the
 * column and a pumparound is negative. Boundary nodes are excluded: the condenser duty is an output and the
 * reboiler already carries its own specification.</p>
 */
public record V3PumparoundSpec(int returnTray, int drawTray, double dutyWatts, Split split) {
    /** Where the authored duty is placed inside the pumparound zone. */
    public enum Split {
        /** The whole duty is absorbed on the return tray, matching a single-stage reference energy stream. */
        RETURN_TRAY,
        /** The duty is divided equally over every tray from the return tray to the draw tray. */
        UNIFORM
    }

    public V3PumparoundSpec {
        split = Objects.requireNonNull(split, "split");
        if (returnTray < 1 || drawTray < returnTray) {
            throw new IllegalArgumentException(
                    "V3 pumparounds require 1 <= returnTray <= drawTray; received returnTray=" + returnTray
                            + ", drawTray=" + drawTray);
        }
        if (!Double.isFinite(dutyWatts) || dutyWatts == 0.0) {
            throw new IllegalArgumentException("V3 pumparound duty must be finite and nonzero");
        }
    }

    /** Number of trays over which this pumparound places its duty. */
    public int trayCount() {
        return split == Split.RETURN_TRAY ? 1 : drawTray - returnTray + 1;
    }

    /** Signed duty placed on one tray of the zone; zero outside it. */
    public double trayDutyWatts(int trayNumber) {
        if (trayNumber < returnTray || trayNumber > drawTray) return 0.0;
        return switch (split) {
            case RETURN_TRAY -> trayNumber == returnTray ? dutyWatts : 0.0;
            case UNIFORM -> dutyWatts / (drawTray - returnTray + 1);
        };
    }

    // Shared prescribed-stage-heat arithmetic; a duty is a constant source term, never a MESH unknown. This is the
    // only place that knows the split rule, so the digest can pin it with a single revision string.

    /** Bumped whenever the tray expansion below changes for an unchanged authored input. */
    static final String SPLIT_RULE_REVISION = "v3-pumparound-split-r1";

    /** Expands the authored pumparounds into signed per-node duties; overlapping zones add. */
    static double[] nodeDutyWatts(V3ColumnInput input, V3ColumnTopology topology) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(topology, "topology");
        double[] duties = new double[topology.nodeCount()];
        for (V3PumparoundSpec pumparound : input.pumparounds()) {
            if (pumparound.drawTray() > topology.trayCount()) {
                throw new IllegalArgumentException("V3 pumparound tray is outside the equilibrium-tray range");
            }
            for (int tray = pumparound.returnTray(); tray <= pumparound.drawTray(); tray++) {
                duties[tray] += pumparound.trayDutyWatts(tray);
            }
        }
        return duties;
    }

    /**
     * Gross authored cooling as a nonpositive value; heating duties are excluded.
     *
     * <p>This is one half of the signed total. Any energy bound built on it has to credit the other half —
     * {@link #totalHeatingWatts(V3ColumnInput)} — because heat added on a tray is heat the coolers below can
     * remove again; see {@link V3HeatFeasibility}.</p>
     */
    static double totalCoolingWatts(V3ColumnInput input) {
        return input.pumparounds().stream().mapToDouble(V3PumparoundSpec::dutyWatts).filter(duty -> duty < 0.0).sum();
    }

    /** Gross authored heating as a nonnegative value; the credit the cooling bounds owe the heaters. */
    static double totalHeatingWatts(V3ColumnInput input) {
        return input.pumparounds().stream().mapToDouble(V3PumparoundSpec::dutyWatts).filter(duty -> duty > 0.0).sum();
    }

    /** Signed total of every authored duty. */
    static double totalDutyWatts(V3ColumnInput input) {
        return input.pumparounds().stream().mapToDouble(V3PumparoundSpec::dutyWatts).sum();
    }

    /** Scales every authored duty by a continuation fraction, preserving geometry and split. */
    static List<V3PumparoundSpec> scaled(V3ColumnInput input, double fraction) {
        if (!Double.isFinite(fraction) || fraction < 0.0 || fraction > 1.0) {
            throw new IllegalArgumentException("V3 pumparound continuation fraction must be within zero to one");
        }
        if (fraction == 0.0) return List.of();
        return input.pumparounds().stream().map(pumparound -> new V3PumparoundSpec(pumparound.returnTray(),
                pumparound.drawTray(), fraction * pumparound.dutyWatts(), pumparound.split())).toList();
    }
}

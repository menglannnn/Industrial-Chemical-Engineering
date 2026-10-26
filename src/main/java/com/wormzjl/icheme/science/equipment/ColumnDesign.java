package com.wormzjl.icheme.science.equipment;

import com.wormzjl.icheme.science.column.v3.V3ColumnInput;
import com.wormzjl.icheme.science.column.v3.V3ColumnSpecification;
import com.wormzjl.icheme.science.column.v3.V3SteamFeedSpec;

import java.util.Objects;

/**
 * A column design saved by a column calculator (batch 2026-10-26-multi-machine-framework, plan section 4.3): the trays,
 * the diameter and the components an equipment constructor builds the column from. Saving checks the structure only; the
 * run checks the thermodynamics.
 *
 * @param trays      the tray layout, by the calculator's numbers
 * @param diameter   the column's diameter, m: the calculator's to the next half metre
 * @param components the component design, its flags resolved from the design's numbers
 */
public record ColumnDesign(ColumnTrays trays, double diameter, ColumnComponents components) {
    public ColumnDesign {
        Objects.requireNonNull(trays, "trays");
        Objects.requireNonNull(components, "components");
        if (!(diameter >= EquipmentShape.MIN_DIAMETER && diameter <= EquipmentShape.MAX_COLUMN_DIAMETER) || diameter * 2 != Math.rint(diameter * 2))
            throw new IllegalArgumentException("A column's diameter is " + EquipmentShape.MIN_DIAMETER + " to " + EquipmentShape.MAX_COLUMN_DIAMETER + " m in half metres, not " + diameter);
        if (components.pumparoundPumps().size() != trays.pumparounds().size())
            throw new IllegalArgumentException("One pumparound pump per pumparound: " + components.pumparoundPumps().size() + " pumps, " + trays.pumparounds().size() + " pumparounds");
    }

    /** The diameter a column is built at from a calculator's sized diameter: to the nearest half metre, within the column sizes. */
    public static double builtDiameter(double sized) {
        return Math.max(EquipmentShape.MIN_DIAMETER, Math.min(EquipmentShape.MAX_COLUMN_DIAMETER, Math.round(sized * 2) / 2.0));
    }

    /**
     * The design of a calculator input with the player's component choices, after the structure checks: the column has a
     * diameter and at least one tray, its draws, steam feeds and pumparounds lie on its trays, every nozzle finds a row on
     * its side of the shell, and a column without reboiler duty is stripped by steam into its sump (the column solver
     * refuses it otherwise: nothing boils up at the bottom).
     *
     * @throws IllegalArgumentException with the reason, in words for the player
     */
    public static ColumnDesign of(V3ColumnInput input, ColumnComponents.Choices choices) {
        Objects.requireNonNull(choices, "choices");
        if (!(input.columnDiameterMetres() > 0)) throw new IllegalArgumentException("set the column diameter; the column is built at it");
        if (input.stageCount() < 1) throw new IllegalArgumentException("a column needs a tray between the condenser and the reboiler");
        var trays = ColumnTrays.of(input);
        if (trays.trays() != input.stageCount() || trays.draws().size() != input.sideDraws().size() || trays.steam().size() != input.steamFeeds().size()
                || trays.pumparounds().size() != input.pumparounds().size())
            throw new IllegalArgumentException("a side draw, steam feed or pumparound lies outside the trays");
        double duty = input.specifications().stream().filter(V3ColumnSpecification.ReboilerDuty.class::isInstance)
                .mapToDouble(s -> ((V3ColumnSpecification.ReboilerDuty) s).watts()).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("the design has no reboiler duty"));
        if (!ColumnComponents.kettleRequired(duty) && !V3SteamFeedSpec.hasSumpFeed(input))
            throw new IllegalArgumentException("without reboiler duty the column needs stripping steam into its bottom tray");
        double diameter = builtDiameter(input.columnDiameterMetres());
        var shape = EquipmentShape.column(diameter, trays, EquipmentShape.Heading.NORTH, EquipmentShape.minimumSkirt(diameter));
        var missing = ColumnStructure.unplaced(shape);
        if (!missing.isEmpty())
            throw new IllegalArgumentException(nozzle(missing.getFirst()) + " has no free row on the " + side(missing.getFirst()) + " side of a "
                    + (input.stageCount() + 2) + "-tray column");
        return new ColumnDesign(trays, diameter, ColumnComponents.resolve(choices, input.topPressurePascal(), duty, trays.pumparounds().size()));
    }

    private static String nozzle(ColumnStructure.Unplaced u) {
        return switch (u.role()) {
            case REFLUX -> "the reflux nozzle";
            case FEED -> "the feed nozzle";
            case PUMPAROUND_DRAW -> "pumparound " + u.index() + "'s draw";
            case RETURN -> "pumparound " + u.index() + "'s return";
            case VAPOUR_RETURN -> "the reboiler's vapour return";
            case DRAW -> "side draw " + u.index();
            case STEAM -> "steam feed " + u.index();
            case BOTTOMS -> "the bottoms nozzle";
            default -> u.role().name().toLowerCase(java.util.Locale.ROOT);
        };
    }

    private static String side(ColumnStructure.Unplaced u) {
        return u.axis() == 2 ? "front" : u.sign() < 0 ? "right" : "left";
    }
}

package com.wormzjl.icheme.science.equipment;

import com.wormzjl.icheme.science.fluid.solver.PhaseLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * The component design of a distillation column (batch 2026-10-26-multi-machine-framework, plan section 4): what the
 * player chose on the column calculator's Components page, and the two components the design's numbers require. The
 * flags are resolved when the design is saved, so a later change of the rules never changes a built column's
 * requirements. The receiver (a lying drum with a boot), the reflux line (its tee and valve, also at a zero reflux ratio,
 * owner answer Q2) and the control valves are always required and carry no field.
 *
 * @param condenser       the condenser kind
 * @param refluxPump      the reflux pump kind
 * @param pumparoundPumps one pump kind per designed pumparound, in the design's order
 * @param ejector         a steam ejector before the condenser: the top pressure is below ambient
 * @param kettle          a kettle reboiler: the design has a reboiler duty
 */
public record ColumnComponents(Condenser condenser, Pump refluxPump, List<Pump> pumparoundPumps, boolean ejector, boolean kettle) {
    /** An air cooler, or an exchanger taking the vapour on its shell side (decision D15: its condensate drains only there). */
    public enum Condenser { AIR_COOLER, EXCHANGER }

    /** A standard pump on its base, or an In-line Pump fitted on a pipe. */
    public enum Pump { STANDARD, IN_LINE }

    public ColumnComponents {
        Objects.requireNonNull(condenser, "condenser");
        Objects.requireNonNull(refluxPump, "refluxPump");
        pumparoundPumps = List.copyOf(pumparoundPumps);
        if (pumparoundPumps.size() > ColumnTrays.MAX_PUMPAROUNDS)
            throw new IllegalArgumentException("A column has at most " + ColumnTrays.MAX_PUMPAROUNDS + " pumparound pumps, not " + pumparoundPumps.size());
    }

    /**
     * The player's choices: the condenser, the reflux pump and one pump per designed pumparound.
     */
    public record Choices(Condenser condenser, Pump refluxPump, List<Pump> pumparoundPumps) {
        /** What a calculator without a saved design proposes: an air cooler and standard pumps. */
        public static final Choices DEFAULT = new Choices(Condenser.AIR_COOLER, Pump.STANDARD, List.of());

        public Choices {
            Objects.requireNonNull(condenser, "condenser");
            Objects.requireNonNull(refluxPump, "refluxPump");
            pumparoundPumps = List.copyOf(pumparoundPumps);
            if (pumparoundPumps.size() > ColumnTrays.MAX_PUMPAROUNDS) throw new IllegalArgumentException("Too many pumparound pumps");
        }

        /** These choices for {@code pumparounds} pumparounds: the chosen pumps kept in order, a further one standard. */
        public Choices fit(int pumparounds) {
            var pumps = new ArrayList<Pump>();
            for (int i = 0; i < pumparounds; i++) pumps.add(i < pumparoundPumps.size() ? pumparoundPumps.get(i) : Pump.STANDARD);
            return new Choices(condenser, refluxPump, pumps);
        }

        public Choices withCondenser(Condenser next) {
            return new Choices(next, refluxPump, pumparoundPumps);
        }

        public Choices withRefluxPump(Pump next) {
            return new Choices(condenser, next, pumparoundPumps);
        }

        /** These choices with pumparound {@code index}'s pump replaced (the list fitted to reach it). */
        public Choices withPumparoundPump(int index, Pump next) {
            var pumps = new ArrayList<>(fit(Math.max(pumparoundPumps.size(), index + 1)).pumparoundPumps());
            pumps.set(index, next);
            return new Choices(condenser, refluxPump, pumps);
        }
    }

    /** The choices this design was saved with: what the Components page shows again. */
    public Choices choices() {
        return new Choices(condenser, refluxPump, pumparoundPumps);
    }

    /**
     * A top pressure below ambient ({@link PhaseLayout#AMBIENT_PRESSURE}) needs a steam ejector before the condenser: it
     * compresses the overhead to about the drum's pressure, where the stripping steam can condense (decision D12).
     */
    public static boolean ejectorRequired(double topPressurePascal) {
        return topPressurePascal < PhaseLayout.AMBIENT_PRESSURE;
    }

    /** A reboiler duty above zero needs a kettle reboiler; without one, sump steam strips the bottom. */
    public static boolean kettleRequired(double reboilerDutyWatts) {
        return reboilerDutyWatts > 0;
    }

    /** The design's components: the choices fitted to {@code pumparounds} pumparounds, the flags from the design's numbers. */
    public static ColumnComponents resolve(Choices choices, double topPressurePascal, double reboilerDutyWatts, int pumparounds) {
        var fitted = choices.fit(pumparounds);
        return new ColumnComponents(fitted.condenser(), fitted.refluxPump(), fitted.pumparoundPumps(),
                ejectorRequired(topPressurePascal), kettleRequired(reboilerDutyWatts));
    }

    /**
     * A design read back from saved names (enums are saved by name, AGENTS.md): each name must be one of its enum's.
     *
     * @throws IllegalArgumentException naming the field whose name is unknown
     */
    public static ColumnComponents named(String condenser, String refluxPump, List<String> pumparoundPumps, boolean ejector, boolean kettle) {
        var pumps = new ArrayList<Pump>();
        for (int i = 0; i < pumparoundPumps.size(); i++) pumps.add(constant(Pump.class, pumparoundPumps.get(i), "pumparound pump " + (i + 1)));
        return new ColumnComponents(constant(Condenser.class, condenser, "condenser"), constant(Pump.class, refluxPump, "reflux pump"), pumps, ejector, kettle);
    }

    private static <E extends Enum<E>> E constant(Class<E> type, String name, String field) {
        try {
            return Enum.valueOf(type, Objects.requireNonNull(name, field).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            throw new IllegalArgumentException(field + ": unknown kind " + name);
        }
    }
}

package com.wormzjl.createcheme.science.equipment;

import java.util.Objects;

/**
 * The auxiliaries a player chooses for a distillation column (owner request and decisions 2026-10-16, batch
 * 2026-10-15-custom-equipment-rendering, {@code COLUMN_AUXILIARIES_PLAN.md}): the overhead condenser, required, cooled by
 * water (a shell-and-tube exchanger on the side structure) or by air (a forced-draft bank on its own stand); a steam
 * ejector in the overhead line before it; and a reboiler. The reflux drum comes with the condenser, the pumparound
 * coolers follow the trays ({@link ColumnTrays#pumparounds()}). What they look like and where they stand is
 * {@link ColumnAuxiliaries}; they are drawn only.
 */
public record ColumnOptions(Condenser condenser, boolean ejector, Reboiler reboiler) {
    /** A new column's: a water-cooled condenser, no ejector, no reboiler. */
    public static final ColumnOptions DEFAULT = new ColumnOptions(Condenser.WATER, false, Reboiler.NONE);

    public enum Condenser {
        WATER, AIR
    }

    public enum Reboiler {
        NONE, KETTLE
    }

    public ColumnOptions {
        Objects.requireNonNull(condenser, "condenser");
        Objects.requireNonNull(reboiler, "reboiler");
    }

    public ColumnOptions withCondenser(Condenser condenser) {
        return new ColumnOptions(condenser, ejector, reboiler);
    }

    public ColumnOptions withEjector(boolean ejector) {
        return new ColumnOptions(condenser, ejector, reboiler);
    }

    public ColumnOptions withReboiler(Reboiler reboiler) {
        return new ColumnOptions(condenser, ejector, reboiler);
    }

    /** For NBT and the network: the condenser in bit 0, the ejector in bit 1, the reboiler from bit 2. */
    public int encode() {
        return condenser.ordinal() | (ejector ? 2 : 0) | reboiler.ordinal() << 2;
    }

    /** @throws IllegalArgumentException for bits that name no choice */
    public static ColumnOptions decode(int bits) {
        int reboiler = bits >> 2;
        if (bits < 0 || reboiler >= Reboiler.values().length) throw new IllegalArgumentException("No column options are encoded as " + bits);
        return new ColumnOptions(Condenser.values()[bits & 1], (bits & 2) != 0, Reboiler.values()[reboiler]);
    }
}

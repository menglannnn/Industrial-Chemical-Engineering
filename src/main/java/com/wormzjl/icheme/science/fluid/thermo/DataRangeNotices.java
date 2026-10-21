package com.wormzjl.icheme.science.fluid.thermo;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongFunction;

/**
 * The data ranges a committed state lies outside (owner rule of 2026-10-16, RANGE_EXTRAPOLATION_PLAN section 5): the
 * models continue there, and the island's status line says so in one advisory line, never a hold. The worker reads them
 * once per committed interval on its end state ({@link FluidThermodynamics#dataRanges}); one notice per subject and side,
 * the first node met in the network's order, so the line is deterministic.
 */
public final class DataRangeNotices {
    /** What the island status line carries after its own status when the committed state lies outside a data range. */
    public static final String STATUS_MARKER = "; outside data range: ";

    /** One data range left: {@code what} was extrapolated below or above {@code bound} ({@code unit}) at network node {@code node}. */
    public record Notice(String what, boolean below, double bound, String unit, long node) {
        public Notice {
            Objects.requireNonNull(what);
            Objects.requireNonNull(unit);
        }

        /** The notice in words, {@code where} the node's label (empty when unknown). */
        public String text(String where) {
            return what + " extrapolated " + (below ? "below " : "above ") + amount(bound, unit)
                    + (where == null || where.isEmpty() ? "" : " at " + where);
        }
    }

    private final Map<String, Notice> notices = new LinkedHashMap<>();

    /** Records {@code value} against the data range {@code [minimum, maximum]} of {@code what} at {@code node}. */
    public void check(String what, double value, double minimum, double maximum, String unit, long node) {
        if (value < minimum) notices.putIfAbsent(what + " below", new Notice(what, true, minimum, unit, node));
        else if (value > maximum) notices.putIfAbsent(what + " above", new Notice(what, false, maximum, unit, node));
    }

    /** The notices in the order they were first met. */
    public List<Notice> notices() { return List.copyOf(notices.values()); }

    /**
     * The status-line suffix of {@code notices}: empty for none, else {@link #STATUS_MARKER}, the first notice and how
     * many more there are, e.g. {@code "; outside data range: Methane liquid viscosity extrapolated below 90.69 K at
     * node 7 (+2 more)"}.
     */
    public static String line(List<Notice> notices, LongFunction<String> names) {
        if (notices.isEmpty()) return "";
        var first = notices.getFirst();
        return STATUS_MARKER + first.text(names.apply(first.node())) + (notices.size() > 1 ? " (+" + (notices.size() - 1) + " more)" : "");
    }

    /** The notice suffix a status line carries ({@link #line}), empty without one. */
    public static String suffix(String status) {
        int at = status.indexOf(STATUS_MARKER);
        return at < 0 ? "" : status.substring(at);
    }

    private static String amount(double value, String unit) {
        if (unit.equals("Pa") && value >= 1e6) return number(value / 1e6) + " MPa";
        if (unit.equals("Pa") && value >= 1e3) return number(value / 1e3) + " kPa";
        return number(value) + " " + unit;
    }

    private static String number(double value) {
        return new BigDecimal(value).round(new MathContext(5)).stripTrailingZeros().toPlainString();
    }
}

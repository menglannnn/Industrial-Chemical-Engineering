package com.wormzjl.createcheme.science.thermo.phase;

import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Which phases compete in an equilibrium calculation: the fluid phases always, plus the named pure crystals, plus
 * gas hydrates if {@link #hydrates()}.
 *
 * <p>A package declares the competitions it is qualified for ({@link PhaseContract#qualifiedCompetitions()}); a
 * request names the competition it needs, and a request outside the package's set is refused
 * ({@link EquilibriumResult.UnsupportedReason#PHASE_COMPETITION_NOT_QUALIFIED}) rather than answered with fewer
 * phases. Competitions are declared as whole sets, never as independent per-crystal switches, so a package cannot admit
 * two crystals separately without admitting (and so qualifying) their joint competition.</p>
 *
 * <p>{@link #FLUID_ONLY} is an explicitly constrained calculation: its result says nothing about solids and is never
 * labelled a complete equilibrium ({@link EquilibriumResult#solidsAssessed()} is false).</p>
 *
 * <p>{@link #FLUID_AND_CRYSTALS} (P4 stage 2a) asks for the fluid phases and every crystal the package admits, without
 * naming them: the service resolves it against its contract ({@link PhaseContract#resolve}) to the package's qualified
 * crystal set, and its results name that resolved competition. On a package that admits no crystal it stays unresolved
 * and is refused as not qualified.</p>
 *
 * @param crystals crystal ids, sorted; empty for fluid phases only
 * @param hydrates whether gas hydrates compete; water chemistry that no P2 water participation models
 * @param packageCrystals the {@link #FLUID_AND_CRYSTALS} request: all of the package's crystals, unnamed (then
 *        {@code crystals} is empty and {@code hydrates} false)
 */
public record PhaseCompetition(SortedSet<String> crystals, boolean hydrates, boolean packageCrystals) {
    public static final PhaseCompetition FLUID_ONLY = new PhaseCompetition(new TreeSet<>(), false);
    /** The fluid phases and every crystal the package admits (P4 stage 2a); resolved by the service's contract. */
    public static final PhaseCompetition FLUID_AND_CRYSTALS = new PhaseCompetition(new TreeSet<>(), false, true);

    public PhaseCompetition {
        TreeSet<String> copy = new TreeSet<>();
        for (String crystal : Objects.requireNonNull(crystals, "crystals")) {
            if (crystal == null || crystal.isBlank()) throw new IllegalArgumentException("Blank crystal id");
            copy.add(crystal);
        }
        if (packageCrystals && (!copy.isEmpty() || hydrates)) {
            throw new IllegalArgumentException("The package-crystals request names no crystal and no hydrates");
        }
        crystals = java.util.Collections.unmodifiableSortedSet(copy);
    }

    /** Named crystals (none: fluid phases only), with or without hydrates. */
    public PhaseCompetition(SortedSet<String> crystals, boolean hydrates) {
        this(crystals, hydrates, false);
    }

    /** The fluid phases and these pure crystals. */
    public static PhaseCompetition withCrystals(String... crystals) {
        return new PhaseCompetition(new TreeSet<>(Set.of(crystals)), false);
    }

    public boolean fluidOnly() { return crystals.isEmpty() && !hydrates && !packageCrystals; }

    /** Whether crystals compete: named ones, or the package's ({@link #FLUID_AND_CRYSTALS}). */
    public boolean crystalsCompete() { return !crystals.isEmpty() || packageCrystals; }

    @Override
    public String toString() {
        if (fluidOnly()) return "fluid-only";
        if (packageCrystals) return "fluid+package-crystals";
        return "fluid+" + String.join("+", crystals) + (hydrates ? "+hydrates" : "");
    }
}

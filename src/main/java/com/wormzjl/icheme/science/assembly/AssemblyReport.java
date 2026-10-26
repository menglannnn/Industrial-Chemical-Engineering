package com.wormzjl.icheme.science.assembly;

import com.wormzjl.icheme.science.assembly.Blueprint.Slot;
import com.wormzjl.icheme.science.equipment.EquipmentShape.Role;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A validation's result (batch 2026-10-26-multi-machine-framework, plan section 5.3): a state per slot, a state per port
 * of the master and its members, and the footprint the report relied on. State, not a fact: a validation computes it
 * again. Minecraft-free.
 *
 * @param slots     every slot of the blueprint, in its order
 * @param ports     every port of the master and of each unit a slot holds
 * @param footprint the positions the report relied on: the walked pipes and fittings
 * @param members   the origins of the units the slots hold, the master's first
 */
public record AssemblyReport(List<SlotReport> slots, List<PortReport> ports, Set<Long> footprint, List<Long> members) {
    /** No position: a fault with no place, a slot that holds nothing. */
    public static final long NONE = Long.MIN_VALUE;

    public AssemblyReport {
        slots = List.copyOf(slots);
        ports = List.copyOf(ports);
        footprint = Set.copyOf(footprint);
        members = List.copyOf(members);
    }

    /** A state's colour on the column GUI: teal for a part in place, yellow for a misplaced one, red for a missing one. */
    public enum Colour {
        TEAL, YELLOW, RED
    }

    /** A slot's state (plan section 5.3), ordered by how much it needs the player: a later state outranks an earlier one. */
    public enum State {
        OK(Colour.TEAL), SHARED(Colour.YELLOW), WRONG_ORDER(Colour.YELLOW), BRANCHED(Colour.YELLOW), EXTRA_VALVE(Colour.YELLOW),
        VALVE_REVERSED(Colour.YELLOW), WRONG_PORT(Colour.YELLOW), WRONG_KIND(Colour.YELLOW), NOT_LOADED(Colour.RED), OPEN_END(Colour.RED),
        MISSING(Colour.RED);

        public final Colour colour;

        State(Colour colour) {
            this.colour = colour;
        }
    }

    /**
     * A slot's state, the unit's origin or the fitting's position that fills it ({@link #NONE} when nothing does), where
     * its fault lies ({@link #NONE} when nowhere in particular), and a detail for its text: a kind or a role by name, or
     * a word such as {@code backwards}.
     */
    public record SlotReport(Slot slot, State state, long member, long at, String detail) {
        public SlotReport {
            Objects.requireNonNull(slot, "slot");
            Objects.requireNonNull(state, "state");
            Objects.requireNonNull(detail, "detail");
        }
    }

    /**
     * A port's state: still needed (its class colour), connected (grey), a utility (advice, its class colour until
     * piped), blind (no colour), blind but piped (a fault), or not used by the design.
     */
    public enum PortState {
        NEEDED, CONNECTED, UTILITY, BLIND, BLIND_PIPED, UNUSED
    }

    /** A port of the unit at {@code unit}: its role, which one of that role, its state. */
    public record PortReport(long unit, Role role, int ordinal, PortState state) {
    }

    /** The slots in place. */
    public int connected() {
        return (int) slots.stream().filter(s -> s.state().colour == Colour.TEAL).count();
    }

    /** The slots misplaced, and the blind ports piped. */
    public int misplaced() {
        return (int) slots.stream().filter(s -> s.state().colour == Colour.YELLOW).count()
                + (int) ports.stream().filter(p -> p.state() == PortState.BLIND_PIPED).count();
    }

    /** The slots missing or not checked. */
    public int missing() {
        return (int) slots.stream().filter(s -> s.state().colour == Colour.RED).count();
    }

    /** Whether the structure is complete: every slot in place and no blind port piped. */
    public boolean complete() {
        return misplaced() == 0 && missing() == 0;
    }

    /** The report of {@code slot}. */
    public SlotReport slot(Slot slot) {
        return slots.stream().filter(s -> s.slot().equals(slot)).findFirst().orElseThrow(() -> new IllegalArgumentException("No slot " + slot));
    }
}

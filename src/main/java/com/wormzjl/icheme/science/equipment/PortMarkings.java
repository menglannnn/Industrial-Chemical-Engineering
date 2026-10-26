package com.wormzjl.icheme.science.equipment;

import com.wormzjl.icheme.science.equipment.EquipmentShape.Kind;
import com.wormzjl.icheme.science.equipment.EquipmentShape.Role;

import java.util.Optional;

/**
 * Which way a process stream passes each nozzle (batch 2026-10-26-multi-machine-framework, plan section 9): in, out, or
 * either way. An assembly's validation reads a unit's inlets and outlets from it, and the direction markers draw it. A
 * marker is a drawing: the fluid network does not enforce a nozzle's direction, as a real nozzle does not.
 */
public final class PortMarkings {
    private PortMarkings() {
    }

    /** A nozzle's stream direction. */
    public enum Flow {
        IN, OUT, NONE
    }

    /**
     * The stream direction at {@code role} on a {@code kind}: a role named an inlet or an outlet by its name, the feeds,
     * returns and suctions in, the products, draws and discharges out. A drum's side connections take its feed (owner
     * answer Q3); a storage tank's or sphere's ports serve either way.
     */
    public static Flow flow(Kind kind, Role role) {
        if (kind.storage()) return Flow.NONE;
        return switch (role) {
            case SIDE -> kind == Kind.DRUM ? Flow.IN : Flow.NONE;
            case FEED, REFLUX, RETURN, STEAM, VAPOUR_RETURN, TUBE_INLET, SHELL_INLET, PROCESS_INLET, SUCTION, MOTIVE, FUEL, CONVECTION_INLET, RADIANT_INLET,
                 NITROGEN, COOLANT_INLET, REGENERATION_INLET, HYDROGEN, QUENCH, CATALYST_INLET, AIR, OXIDANT, WATER, CUTTING_WATER, SOLIDS_INLET, FEEDWATER -> Flow.IN;
            case VAPOUR_OUTLET, LIQUID_OUTLET, BOOT_DRAIN, DRAW, PUMPAROUND_DRAW, BOTTOMS, TUBE_OUTLET, SHELL_OUTLET, PROCESS_OUTLET, DISCHARGE, CONVECTION_OUTLET,
                 RADIANT_OUTLET, PRODUCT, COOLANT_OUTLET, REGENERATION_OUTLET, EFFLUENT, VENT, EXHAUST, FLUE_GAS, RELIEF, SYNGAS, DRAIN, CONDENSATE,
                 CATALYST_OUTLET, SOLIDS_OUTLET, BLOWDOWN -> Flow.OUT;
            case BOTTOM, OVERFLOW -> Flow.NONE;
        };
    }

    /**
     * The nozzle a stream entering a pass-through unit at {@code role} leaves by, or empty: a mover's or an ejector's
     * suction and discharge, an exchanger's or a kettle's tube side, an exchanger's shell side, an air cooler's process
     * line, and back the same way for a unit met from its outlet.
     */
    public static Optional<Role> partner(Kind kind, Role role) {
        boolean pump = kind.mover() || kind == Kind.EJECTOR;
        boolean tubes = kind == Kind.EXCHANGER || kind == Kind.KETTLE;
        return Optional.ofNullable(switch (role) {
            case SUCTION -> pump ? Role.DISCHARGE : null;
            case DISCHARGE -> pump ? Role.SUCTION : null;
            case TUBE_INLET -> tubes ? Role.TUBE_OUTLET : null;
            case TUBE_OUTLET -> tubes ? Role.TUBE_INLET : null;
            case SHELL_INLET -> kind == Kind.EXCHANGER ? Role.SHELL_OUTLET : null;
            case SHELL_OUTLET -> kind == Kind.EXCHANGER ? Role.SHELL_INLET : null;
            case PROCESS_INLET -> kind == Kind.AIR_COOLER ? Role.PROCESS_OUTLET : null;
            case PROCESS_OUTLET -> kind == Kind.AIR_COOLER ? Role.PROCESS_INLET : null;
            default -> null;
        });
    }
}

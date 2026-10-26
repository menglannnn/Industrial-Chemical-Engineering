package com.wormzjl.icheme.science.equipment;

import com.wormzjl.icheme.science.equipment.EquipmentShape.Kind;
import com.wormzjl.icheme.science.equipment.EquipmentShape.Role;
import com.wormzjl.icheme.science.equipment.PortMarkings.Flow;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Which way a stream passes each nozzle (batch 2026-10-26-multi-machine-framework, plan section 9). */
class PortMarkingsTest {
    @Test
    void aPassThroughUnitsPartnersPairOneInletWithOneOutlet() {
        for (var kind : Kind.values())
            for (var role : Role.values()) {
                var partner = PortMarkings.partner(kind, role);
                if (partner.isEmpty()) continue;
                assertEquals(Optional.of(role), PortMarkings.partner(kind, partner.get()), () -> kind + " " + role);
                assertNotEquals(PortMarkings.flow(kind, role), PortMarkings.flow(kind, partner.get()), () -> kind + " " + role);
                assertNotEquals(Flow.NONE, PortMarkings.flow(kind, role), () -> kind + " " + role);
            }
    }

    @Test
    void theColumnsFeedsAndReturnsComeInAndItsProductsGoOut() {
        for (var role : new Role[]{Role.FEED, Role.REFLUX, Role.RETURN, Role.STEAM, Role.VAPOUR_RETURN})
            assertEquals(Flow.IN, PortMarkings.flow(Kind.COLUMN, role), role::name);
        for (var role : new Role[]{Role.VAPOUR_OUTLET, Role.DRAW, Role.PUMPAROUND_DRAW, Role.BOTTOMS})
            assertEquals(Flow.OUT, PortMarkings.flow(Kind.COLUMN, role), role::name);
    }

    @Test
    void aDrumsSideConnectionsTakeItsFeedAndAStorageVesselsPortsServeEitherWay() {
        assertEquals(Flow.IN, PortMarkings.flow(Kind.DRUM, Role.SIDE));
        assertEquals(Flow.OUT, PortMarkings.flow(Kind.DRUM, Role.BOOT_DRAIN));
        for (var role : Role.values()) {
            assertEquals(Flow.NONE, PortMarkings.flow(Kind.TANK, role));
            assertEquals(Flow.NONE, PortMarkings.flow(Kind.SPHERE, role));
        }
    }

    @Test
    void anEjectorPassesItsSuctionToItsDischargeAndTakesItsMotiveSteamApart() {
        assertEquals(Optional.of(Role.DISCHARGE), PortMarkings.partner(Kind.EJECTOR, Role.SUCTION));
        assertEquals(Optional.empty(), PortMarkings.partner(Kind.EJECTOR, Role.MOTIVE));
        assertEquals(Flow.IN, PortMarkings.flow(Kind.EJECTOR, Role.MOTIVE));
        // A kettle's tube bundle passes through; its shell holds the boiling liquid and has no shell nozzles.
        assertEquals(Optional.of(Role.TUBE_OUTLET), PortMarkings.partner(Kind.KETTLE, Role.TUBE_INLET));
        assertEquals(Optional.empty(), PortMarkings.partner(Kind.KETTLE, Role.SHELL_INLET));
    }
}

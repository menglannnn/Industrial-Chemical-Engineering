package com.wormzjl.createcheme.science.equipment;

import com.wormzjl.createcheme.science.equipment.EquipmentShape.Kind;

/** Default supports for the in-game scene. */
public class Supports {
    public static void main(String[] args) {
        Object[][] units = {{Kind.VERTICAL_HEATER, 2.5, 4}, {Kind.VERTICAL_HEATER, 4.0, 9}, {Kind.EXCHANGER, .5, 2}, {Kind.KETTLE, .5, 2}, {Kind.EJECTOR, .05, 1}, {Kind.CABIN_HEATER, 4.0, 6}};
        for (var u : units) {
            var k = (Kind) u[0];
            double d = (Double) u[1];
            int l = (Integer) u[2];
            System.out.println(k + " " + d + " " + l + " support " + ProcessUnit.defaultSupport(k, d, l));
        }
    }
}

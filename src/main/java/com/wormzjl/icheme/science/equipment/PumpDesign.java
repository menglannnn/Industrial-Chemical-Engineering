package com.wormzjl.icheme.science.equipment;

import com.wormzjl.icheme.science.equipment.EquipmentShape.Orientation;

import java.util.List;

/**
 * A centrifugal pump by its discharge ({@link ProcessUnit#pump}): API 610 OH2 up to {@link ProcessUnit#OH2_MAX}, BB1
 * axially split above, up to 0.75 m. Its plinth stands at grade; the constructor's Up lifts it onto a deck.
 */
final class PumpDesign extends UnitDesign {
    @Override
    public double minSize() {
        return ProcessUnit.SUCTION_STEP;
    }

    @Override
    public double maxSize() {
        // The largest discharge whose suction, one size larger (ProcessUnit.pumpSuction), is a pipe's largest.
        return .6;
    }

    @Override
    public double sizeStep() {
        return ProcessUnit.SUCTION_STEP;
    }

    @Override
    public int minSupport(double size, int length) {
        return 0;
    }

    @Override
    public int maxSupport() {
        return 0;
    }

    @Override
    public Orientation orientation() {
        return Orientation.HORIZONTAL;
    }

    @Override
    public boolean moves() {
        return true;
    }

    @Override
    public String sizeRow() {
        return "pump_discharge";
    }

    @Override
    public String supportRow() {
        return null;
    }

    @Override
    public Line held(EquipmentShape shape) {
        double d = shape.diameter();
        return new Line("pump_flow", whole(ProcessUnit.pumpFlow(d)), whole(ProcessUnit.pumpPower(d)));
    }

    @Override
    public String variant(EquipmentShape shape) {
        return shape.diameter() > ProcessUnit.OH2_MAX + 1e-9 ? ".split" : "";
    }

    @Override
    public List<Line> datasheet(EquipmentShape shape) {
        double d = shape.diameter();
        return List.of(new Line("pump", size(d), size(ProcessUnit.pumpSuction(d))), new Line("pump_duty", whole(ProcessUnit.pumpFlow(d)), whole(ProcessUnit.pumpPower(d))));
    }

    @Override
    void build(ProcessUnit unit) {
        unit.pump();
    }
}

package com.wormzjl.icheme.science.equipment;

import com.wormzjl.icheme.science.equipment.EquipmentShape.Orientation;

import java.util.List;

/** A steam ejector standing on its frame ({@link ProcessUnit#ejector}): suction 0.05 to 1 m, 8.3 suctions long. */
final class EjectorDesign extends UnitDesign {
    @Override
    public double minSize() {
        return ProcessUnit.SUCTION_STEP;
    }

    @Override
    public double maxSize() {
        return 1;
    }

    @Override
    public double sizeStep() {
        return ProcessUnit.SUCTION_STEP;
    }

    /** The discharge flange 1 m up at least; a new ejector's 2 m. */
    @Override
    public int minSupport(double size, int length) {
        return 1;
    }

    @Override
    public int defaultSupport(double size, int length) {
        return 2;
    }

    @Override
    public Orientation orientation() {
        return Orientation.VERTICAL;
    }

    @Override
    public String sizeRow() {
        return "suction";
    }

    @Override
    public String supportRow() {
        return "discharge";
    }

    @Override
    public Line held(EquipmentShape shape) {
        return new Line("ejector_length", number(ProcessUnit.ejectorLength(shape.diameter())));
    }

    @Override
    public List<Line> datasheet(EquipmentShape shape) {
        return List.of(new Line("suction", size(shape.diameter()), number(ProcessUnit.ejectorLength(shape.diameter()))));
    }

    @Override
    void build(ProcessUnit unit) {
        unit.ejector();
    }
}

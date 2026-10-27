import com.wormzjl.createcheme.science.equipment.EquipmentShape;
import com.wormzjl.createcheme.science.equipment.ShapeRasterizer;

/** Prints the nozzle cells of the check drum (D 3 m, T/T 6 m, skirt 2 m, front north) and its box size. */
public class Ports {
    public static void main(String[] args) {
        var shape = EquipmentShape.drum(3, 4, EquipmentShape.Orientation.VERTICAL, EquipmentShape.Heading.NORTH, 2);
        System.out.println("size " + shape.sizeX() + " " + shape.sizeY() + " " + shape.sizeZ());
        for (var p : ShapeRasterizer.ports(shape)) System.out.println(p);
        System.out.println("fluid cell " + ShapeRasterizer.fluidCell(shape));
    }
}

import com.wormzjl.createcheme.science.equipment.EquipmentShape;
public class Sizes {
    public static void main(String[] args) {
        var shape = EquipmentShape.drum(3, 4, EquipmentShape.Orientation.VERTICAL, EquipmentShape.Heading.NORTH, 2);
        for (var n : shape.nozzles()) System.out.println(n.role() + " maxOd " + n.maxOd() + " defaultOd " + n.defaultOd());
    }
}

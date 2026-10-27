import com.wormzjl.icheme.science.equipment.ColumnComponents;
import com.wormzjl.icheme.science.equipment.ColumnDesign;
import com.wormzjl.icheme.science.equipment.ColumnTrays;
import com.wormzjl.icheme.world.level.block.entity.EquipmentConstructorBlockEntity;
import java.util.List;
import net.minecraft.nbt.CompoundTag;

/** The F3 in-game check's constructor: the review column (2 m, 20 trays) imported, as block-entity SNBT for a setblock. */
public final class ConstructorSnbt {
    public static void main(String[] args) {
        var components = new ColumnComponents(ColumnComponents.Condenser.AIR_COOLER, ColumnComponents.Pump.STANDARD, List.of(ColumnComponents.Pump.STANDARD), true, true);
        var trays = new ColumnTrays(20, 12, List.of(9), List.of(21), List.of(new ColumnTrays.Pumparound(7, 5)), components);
        var design = EquipmentConstructorBlockEntity.Design.DEFAULT.imported(new ColumnDesign(trays, 2));
        var tag = new CompoundTag();
        EquipmentConstructorBlockEntity.writeDesign(tag, design);
        System.out.println(tag);
    }
}

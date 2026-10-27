import com.wormzjl.icheme.science.equipment.*;
import java.util.List;
/** Scratch: which 0.5 m column designs place a feed, and its largest bore. */
public class SmallColumn {
    public static void main(String[] a) {
        for (int n : new int[]{3, 5, 8, 10}) for (int f : new int[]{2, 3, 4}) {
            if (f > n + 1) continue;
            try {
                var s = EquipmentShape.column(.5, new ColumnTrays(n, f, List.of(), List.of(), List.of()), EquipmentShape.Heading.NORTH, EquipmentShape.minimumSkirt(.5));
                var b = new StringBuilder("n " + n + " feed " + f + " misfit " + ColumnDesign.misfit(s).isPresent() + ":");
                for (var p : ShapeRasterizer.ports(s)) b.append(' ').append(s.nozzles().get(p.nozzle()).role()).append('=').append(NozzleBore.largest(s, p.nozzle()));
                System.out.println(b);
            } catch (IllegalArgumentException e) { System.out.println("n " + n + " feed " + f + " " + e.getMessage()); }
        }
    }
}

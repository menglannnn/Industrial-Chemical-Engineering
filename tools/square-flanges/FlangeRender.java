import com.wormzjl.icheme.science.equipment.LineGeometry;
import com.wormzjl.icheme.science.equipment.Mesh;
import com.wormzjl.icheme.science.equipment.PipeSizes;
import com.wormzjl.icheme.science.fluid.topology.TopologyCompiler.Kind;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;

/** Offline side-profile render of actual pipe polygons; no Minecraft client, world or simulation. */
public final class FlangeRender {
    public static void main(String[] args) throws Exception {
        int code = PipeSizes.code(.4);
        var key = new LineGeometry.Key(Kind.PIPE, 5, 1 << 4 | 1 << 5, code,
                (long) code << 28 | (long) code << 35, 0, true);
        var polygons = new ArrayList<>(LineGeometry.polygons(key));
        // Camera is on -z. Paint far surfaces first, using exact side projection to show the shoulder's angle.
        polygons.sort(Comparator.comparingDouble(FlangeRender::depth).reversed());
        var picture = new BufferedImage(1000, 600, BufferedImage.TYPE_INT_RGB);
        var graphics = picture.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(new Color(239, 243, 247));
        graphics.fillRect(0, 0, picture.getWidth(), picture.getHeight());
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 25));
        graphics.setColor(new Color(31, 41, 51));
        graphics.drawString(args[1], 36, 45);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 17));
        graphics.drawString("Actual pipe mesh · 0.4 m outside diameter · flange at spool joint", 36, 77);
        for (var polygon : polygons) {
            var path = new Path2D.Double();
            var positions = polygon.positions();
            double x0 = positions[0], y0 = positions[1], z0 = positions[2];
            double ax = positions[3] - x0, ay = positions[4] - y0, az = positions[5] - z0;
            double bx = positions[6] - x0, by = positions[7] - y0, bz = positions[8] - z0;
            double nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
            double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (length == 0 || nz > 1e-10) continue;
            for (int i = 0; i < polygon.size(); i++) {
                double x = 500 + 760 * (positions[3 * i] - .5);
                double y = 345 - 760 * (positions[3 * i + 1] - .5);
                if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
            }
            path.closePath();
            int base = switch (polygon.material()) {
                case FLANGE -> 124;
                case BOLT -> 66;
                case OPENING -> 24;
                default -> 153;
            };
            double light = .5 + .5 * Math.max(0, (nx * -.3 + ny * .6 - nz * .74) / length);
            int shade = (int) (base * light);
            graphics.setColor(new Color(shade, Math.min(255, shade + 6), Math.min(255, shade + 12)));
            graphics.fill(path);
        }
        graphics.dispose();
        var file = new File(args[0]);
        file.getAbsoluteFile().getParentFile().mkdirs();
        ImageIO.write(picture, "png", file);
        System.out.println("pipe OD=" + PipeSizes.outsideOf(code) + ", neck OD=" + PipeSizes.hubOutside(PipeSizes.outsideOf(code))
                + ", flange OD=" + PipeSizes.flange(PipeSizes.outsideOf(code)) + ", polygons=" + polygons.size());
    }

    private static double depth(Mesh.Polygon polygon) {
        double sum = 0;
        for (int i = 0; i < polygon.size(); i++) sum += polygon.positions()[3 * i + 2];
        return sum / polygon.size();
    }
}

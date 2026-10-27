import javax.imageio.ImageIO;
import java.io.File;

/** Pixels that differ (any channel by more than 24) between two shots: count and bounding box. Args: a.png b.png [x y w h]. */
public class Diff {
    public static void main(String[] a) throws Exception {
        var p = ImageIO.read(new File(a[0]));
        var q = ImageIO.read(new File(a[1]));
        int x0 = a.length > 2 ? Integer.parseInt(a[2]) : 0, y0 = a.length > 2 ? Integer.parseInt(a[3]) : 0;
        int w = a.length > 2 ? Integer.parseInt(a[4]) : p.getWidth(), h = a.length > 2 ? Integer.parseInt(a[5]) : p.getHeight();
        int n = 0, minX = 9999, minY = 9999, maxX = -1, maxY = -1;
        for (int y = y0; y < y0 + h; y++)
            for (int x = x0; x < x0 + w; x++) {
                int c = p.getRGB(x, y), d = q.getRGB(x, y), m = 0;
                for (int s = 0; s < 24; s += 8) m = Math.max(m, Math.abs(((c >> s) & 255) - ((d >> s) & 255)));
                if (m > 24) { n++; minX = Math.min(minX, x); minY = Math.min(minY, y); maxX = Math.max(maxX, x); maxY = Math.max(maxY, y); }
            }
        System.out.printf("%s vs %s: %d px differ, box x %d-%d y %d-%d%n", a[0], a[1], n, minX, maxX, minY, maxY);
    }
}

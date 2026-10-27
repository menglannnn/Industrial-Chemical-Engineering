import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/** Kit in-game check: old.png new.png out.png. Writes old (left half) and new (right half) at half size on top, and the new shot with changed pixels (any channel > 24) in magenta below, at full size. */
public class KDiffMap {
    public static void main(String[] a) throws Exception {
        var p = ImageIO.read(new File(a[0]));
        var q = ImageIO.read(new File(a[1]));
        int w = q.getWidth(), h = q.getHeight();
        var out = new BufferedImage(w, h + h / 2, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h / 2; y++)
            for (int x = 0; x < w / 2; x++) {
                out.setRGB(x, y, p.getRGB(2 * x, 2 * y));
                out.setRGB(w / 2 + x, y, q.getRGB(2 * x, 2 * y));
            }
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                int c = p.getRGB(x, y), d = q.getRGB(x, y), m = 0;
                for (int s = 0; s < 24; s += 8) m = Math.max(m, Math.abs(((c >> s) & 255) - ((d >> s) & 255)));
                out.setRGB(x, h / 2 + y, m > 24 ? 0xFF00FF : d);
            }
        ImageIO.write(out, "png", new File(a[2]));
    }
}

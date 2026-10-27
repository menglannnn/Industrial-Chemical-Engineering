import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Mean luminance of a region in two shots and the brightest column profile difference. Args: a.png b.png x0 y0 x1 y1. */
public class ShotDiff {
    public static void main(String[] a) throws Exception {
        BufferedImage p = ImageIO.read(new File(a[0])), s = ImageIO.read(new File(a[1]));
        int x0 = Integer.parseInt(a[2]), y0 = Integer.parseInt(a[3]), x1 = Integer.parseInt(a[4]), y1 = Integer.parseInt(a[5]);
        double sp = 0, ss = 0, max = 0; int n = 0;
        StringBuilder prof = new StringBuilder();
        for (int x = x0; x < x1; x++) {
            double cp = 0, cs = 0;
            for (int y = y0; y < y1; y++) { cp += lum(p.getRGB(x, y)); cs += lum(s.getRGB(x, y)); n++; }
            sp += cp; ss += cs; cp /= (y1 - y0); cs /= (y1 - y0);
            max = Math.max(max, cs - cp);
            if ((x - x0) % Math.max(1, (x1 - x0) / 12) == 0) prof.append(String.format(" %.0f/%.0f", cp, cs));
        }
        System.out.printf("mean %.1f -> %.1f (%+.1f %%), largest column gain %.1f%n  columns p/s:%s%n", sp / n, ss / n, 100 * (ss - sp) / sp, max, prof);
    }
    static double lum(int rgb) { return .2126 * (rgb >> 16 & 255) + .7152 * (rgb >> 8 & 255) + .0722 * (rgb & 255); }
}

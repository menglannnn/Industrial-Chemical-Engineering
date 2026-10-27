import javax.imageio.ImageIO;
import java.io.File;
/** Near-black pixels (every channel below 24) per shot, with their bounding box. Args: shots... */
public class Dark {
    public static void main(String[] a) throws Exception {
        for (String f : a) {
            var p = ImageIO.read(new File(f)); int n = 0, x0 = 9999, y0 = 9999, x1 = -1, y1 = -1;
            for (int y = 0; y < p.getHeight(); y++) for (int x = 0; x < p.getWidth(); x++) {
                int c = p.getRGB(x, y); if (((c >> 16) & 255) < 24 && ((c >> 8) & 255) < 24 && (c & 255) < 24) { n++; x0 = Math.min(x0, x); y0 = Math.min(y0, y); x1 = Math.max(x1, x); y1 = Math.max(y1, y); } }
            System.out.printf("%-40s %6d dark px  box x %d-%d y %d-%d%n", new File(f).getName(), n, x0, x1, y0, y1);
        }
    }
}

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
/** Crop {@code in} to x0 y0 x1 y1 and scale it up by an integer factor: java Crop.java in out x0 y0 x1 y1 scale. */
public class Crop {
    public static void main(String[] a) throws Exception {
        var in = ImageIO.read(new File(a[0]));
        int x0 = Integer.parseInt(a[2]), y0 = Integer.parseInt(a[3]), x1 = Integer.parseInt(a[4]), y1 = Integer.parseInt(a[5]), s = Integer.parseInt(a[6]);
        var out = new BufferedImage((x1 - x0) * s, (y1 - y0) * s, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < out.getHeight(); y++) for (int x = 0; x < out.getWidth(); x++) out.setRGB(x, y, in.getRGB(x0 + x / s, y0 + y / s));
        ImageIO.write(out, "png", new File(a[1]));
    }
}

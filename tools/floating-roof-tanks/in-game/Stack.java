import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/** Stack the same crop of several shots, scaled: out x y w h scale in... */
public class Stack {
    public static void main(String[] a) throws Exception {
        int x = Integer.parseInt(a[1]), y = Integer.parseInt(a[2]), w = Integer.parseInt(a[3]), h = Integer.parseInt(a[4]), s = Integer.parseInt(a[5]);
        int n = a.length - 6;
        var out = new BufferedImage(w * s, (h * s + 4) * n, BufferedImage.TYPE_INT_RGB);
        for (int k = 0; k < n; k++) {
            var in = ImageIO.read(new File(a[6 + k]));
            for (int j = 0; j < h * s; j++)
                for (int i = 0; i < w * s; i++) out.setRGB(i, k * (h * s + 4) + j, in.getRGB(x + i / s, y + j / s));
        }
        ImageIO.write(out, "png", new File(a[0]));
    }
}

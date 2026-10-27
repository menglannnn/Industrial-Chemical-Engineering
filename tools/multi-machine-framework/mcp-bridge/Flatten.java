import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/** Bridge screenshots carry alpha 0 on every pixel; this writes an opaque copy: java tmp/w4/Flatten.java in.png out.png */
public class Flatten {
    public static void main(String[] args) throws Exception {
        var in = ImageIO.read(new File(args[0]));
        var out = new BufferedImage(in.getWidth(), in.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < in.getHeight(); y++)
            for (int x = 0; x < in.getWidth(); x++) out.setRGB(x, y, in.getRGB(x, y) & 0xFFFFFF);
        ImageIO.write(out, "png", new File(args[1]));
    }
}

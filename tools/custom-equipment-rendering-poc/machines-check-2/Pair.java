import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Two crops side by side with labels. Args: a.png b.png x0 y0 x1 y1 labelA labelB out.png. */
public class Pair {
    public static void main(String[] a) throws Exception {
        BufferedImage p = ImageIO.read(new File(a[0])), s = ImageIO.read(new File(a[1]));
        int x0 = Integer.parseInt(a[2]), y0 = Integer.parseInt(a[3]), w = Integer.parseInt(a[4]) - x0, h = Integer.parseInt(a[5]) - y0;
        var out = new BufferedImage(2 * w + 6, h, BufferedImage.TYPE_INT_RGB);
        var g = out.createGraphics();
        g.setColor(Color.BLACK); g.fillRect(0, 0, out.getWidth(), h);
        g.drawImage(p.getSubimage(x0, y0, w, h), 0, 0, null);
        g.drawImage(s.getSubimage(x0, y0, w, h), w + 6, 0, null);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16)); g.setColor(Color.BLACK);
        g.drawString(a[6], 8, 20); g.drawString(a[7], w + 14, 20);
        ImageIO.write(out, "png", new File(a[8]));
    }
}

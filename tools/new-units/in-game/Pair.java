import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
/** Side by side: out labelA a.png labelB b.png [x y w h] crop. */
public class Pair {
    public static void main(String[] a) throws Exception {
        var p = ImageIO.read(new File(a[2])); var q = ImageIO.read(new File(a[4]));
        int x = a.length > 5 ? Integer.parseInt(a[5]) : 0, y = a.length > 5 ? Integer.parseInt(a[6]) : 0;
        int w = a.length > 5 ? Integer.parseInt(a[7]) : p.getWidth(), h = a.length > 5 ? Integer.parseInt(a[8]) : p.getHeight();
        var out = new BufferedImage(2 * w + 6, h, BufferedImage.TYPE_INT_RGB);
        var g = out.createGraphics();
        g.setColor(Color.BLACK); g.fillRect(0, 0, out.getWidth(), h);
        g.drawImage(p.getSubimage(x, y, w, h), 0, 0, null); g.drawImage(q.getSubimage(x, y, w, h), w + 6, 0, null);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        for (int k = 0; k < 2; k++) { int ox = k * (w + 6) + 6; String s = a[1 + 2 * k];
            g.setColor(Color.WHITE); for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) g.drawString(s, ox + dx, 18 + dy);
            g.setColor(Color.BLACK); g.drawString(s, ox, 18); }
        ImageIO.write(out, "png", new File(a[0]));
    }
}

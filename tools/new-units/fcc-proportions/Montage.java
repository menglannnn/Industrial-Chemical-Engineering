import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/** Montage out.png title label1 img1 label2 img2 ...: images side by side, bottom-aligned (grade), labels on top. */
public class Montage {
    public static void main(String[] a) throws Exception {
        int n = (a.length - 2) / 2, gap = 30, top = 70, w = gap, h = 0;
        var images = new BufferedImage[n];
        for (int k = 0; k < n; k++) {
            images[k] = ImageIO.read(new File(a[3 + 2 * k]));
            w += images[k].getWidth() + gap;
            h = Math.max(h, images[k].getHeight());
        }
        var out = new BufferedImage(w, h + top + 10, BufferedImage.TYPE_INT_RGB);
        var g = out.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, out.getWidth(), out.getHeight());
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.BOLD, 26));
        g.drawString(a[1], gap, 30);
        g.setFont(new Font("SansSerif", Font.PLAIN, 22));
        int x = gap;
        for (int k = 0; k < n; k++) {
            g.drawString(a[2 + 2 * k], x, 60);
            g.drawImage(images[k], x, top + h - images[k].getHeight(), null);
            x += images[k].getWidth() + gap;
        }
        ImageIO.write(out, "png", new File(a[0]));
    }
}

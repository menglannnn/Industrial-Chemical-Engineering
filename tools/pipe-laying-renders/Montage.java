import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/** Before/after sheet: java Montage.java out.png before.png after.png [x0 y0 x1 y1 scale] — side by side, labelled; a crop region scales up. */
public class Montage {
    public static void main(String[] a) throws Exception {
        BufferedImage before = ImageIO.read(new File(a[1])), after = ImageIO.read(new File(a[2]));
        int x0 = 0, y0 = 0, x1 = before.getWidth(), y1 = before.getHeight();
        double scale = 1;
        if (a.length > 3) {
            x0 = Integer.parseInt(a[3]); y0 = Integer.parseInt(a[4]); x1 = Integer.parseInt(a[5]); y1 = Integer.parseInt(a[6]);
            scale = Double.parseDouble(a[7]);
        }
        int w = (int) ((x1 - x0) * scale), h = (int) ((y1 - y0) * scale), bar = 40;
        boolean stack = w > 1100;
        var out = new BufferedImage(stack ? w : 2 * w + 10, stack ? 2 * (h + bar) : h + bar, BufferedImage.TYPE_INT_RGB);
        var g = out.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, out.getWidth(), out.getHeight());
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        String[] labels = {"Before", "After (sturdy)"};
        BufferedImage[] images = {before, after};
        for (int i = 0; i < 2; i++) {
            int ox = stack ? 0 : i * (w + 10), oy = stack ? i * (h + bar) : 0;
            g.setColor(new Color(0x2C2C2A));
            g.drawString(labels[i], ox + 12, oy + 28);
            g.drawImage(images[i], ox, oy + bar, ox + w, oy + bar + h, x0, y0, x1, y1, null);
        }
        g.dispose();
        ImageIO.write(out, "png", new File(a[0]));
        System.out.println("wrote " + a[0]);
    }
}

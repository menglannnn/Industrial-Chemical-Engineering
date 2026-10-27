import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Editable, deterministic source graphic for the custom clear process-instrument pane; no borrowed texture. */
public final class GlassTexture {
    public static void main(String[] args) throws Exception {
        var image = new BufferedImage(64, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.setColor(new Color(234, 239, 239, 2));g.fillRect(0, 0, 64, 256);
        // Thin neutral edge reflections; there is no opaque border or metre-repeating cyan motif.
        g.setColor(new Color(241, 245, 243, 55));g.fillRect(0, 0, 1, 256);g.fillRect(63, 0, 1, 256);
        g.setColor(new Color(229, 237, 235, 20));g.fillRect(1, 0, 2, 256);g.fillRect(61, 0, 2, 256);
        // Keep the reflections legible against tank metal without clouding the transparent centre.
        g.setColor(new Color(249, 250, 246, 42));
        g.fill(new Polygon(new int[]{12, 16, 26, 22}, new int[]{18, 18, 232, 232}, 4));
        g.setColor(new Color(246, 249, 246, 32));
        g.fill(new Polygon(new int[]{37, 40, 46, 43}, new int[]{38, 38, 204, 204}, 4));
        g.dispose();
        ImageIO.write(image, "png", Path.of(args[0]).toFile());
        System.out.println("Custom clear gauge glass: 64x256 RGBA, alpha 2..55, neutral highlights");
    }
}

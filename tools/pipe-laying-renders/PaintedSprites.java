import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * The painted line sprites (batch 2026-10-26-pipe-laying-system): a bare sprite turned neutral grey with its grain kept,
 * scaled to a mean value, so the block tint {@code Paint.tint} shows the paint on it. A pipe at the painted sprites' mean
 * ({@code Paint.SPRITE_VALUE}, 236) shows the paint itself; a flange at a lower mean shows a shade of it.
 *
 * <p>Args: {@code in.png out.png mean}, for example
 * {@code equipment_pipe.png painted_pipe.png 236} and {@code equipment_flange.png painted_flange.png 200}.</p>
 */
public class PaintedSprites {
    public static void main(String[] args) throws Exception {
        var in = ImageIO.read(new File(args[0]));
        double target = Double.parseDouble(args[2]), sum = 0;
        int n = 0;
        for (int y = 0; y < in.getHeight(); y++)
            for (int x = 0; x < in.getWidth(); x++) {
                int p = in.getRGB(x, y);
                if (p >>> 24 == 0) continue;
                sum += value(p);
                n++;
            }
        double scale = target / (sum / n);
        var out = new BufferedImage(in.getWidth(), in.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < in.getHeight(); y++)
            for (int x = 0; x < in.getWidth(); x++) {
                int p = in.getRGB(x, y);
                int v = (int) Math.min(255, Math.round(value(p) * scale));
                out.setRGB(x, y, p & 0xFF000000 | v << 16 | v << 8 | v);
            }
        ImageIO.write(out, "png", new File(args[1]));
        System.out.printf("%s: mean %.1f -> %.0f (x%.3f)%n", args[1], sum / n, target, scale);
    }

    static double value(int p) {
        return ((p >> 16 & 255) + (p >> 8 & 255) + (p & 255)) / 3.0;
    }
}

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Makes a tinted sprite neutral for the paint tint (batch 2026-10-15-custom-equipment-rendering, paint step): each texel
 * becomes its luma, scaled so the sprite's mean is VesselTextures.NEUTRAL (236), the value the game's tint assumes.
 * Run: java tools/custom-equipment-rendering-poc/textures/Neutral.java <in.png> <out.png> [...pairs]
 */
public class Neutral {
    static final double NEUTRAL = 236;

    public static void main(String[] args) throws Exception {
        for (int k = 0; k + 1 < args.length; k += 2) {
            var in = ImageIO.read(new File(args[k]));
            int w = in.getWidth(), h = in.getHeight();
            double[] luma = new double[w * h];
            double sum = 0;
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++) {
                    int rgb = in.getRGB(x, y);
                    luma[y * w + x] = .2126 * (rgb >> 16 & 0xFF) + .7152 * (rgb >> 8 & 0xFF) + .0722 * (rgb & 0xFF);
                    sum += luma[y * w + x];
                }
            double scale = NEUTRAL / (sum / luma.length);
            var out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            for (int i = 0; i < luma.length; i++) {
                int v = (int) Math.max(0, Math.min(255, Math.round(luma[i] * scale)));
                out.setRGB(i % w, i / w, v << 16 | v << 8 | v);
            }
            ImageIO.write(out, "png", new File(args[k + 1]));
            System.out.printf("%s: mean %.1f, scaled by %.3f%n", args[k], sum / luma.length, scale);
        }
    }
}

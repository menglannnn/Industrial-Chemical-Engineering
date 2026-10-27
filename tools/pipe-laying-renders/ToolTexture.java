import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Draws the pipe laying tool's 32 x 32 item texture (batch 2026-10-26-pipe-laying-system) in the style of
 * accessory_wrench.png: flat body tones, a light streak on the upper-left side of each part, a dark one-pixel outline.
 * A steel shaft on the diagonal with an orange grip, and a pipe spool with two flanges across its tip.
 *
 * <p>Args: {@code out.png [preview.png]}; the preview is the texture at 8 x on a checkerboard.</p>
 */
public class ToolTexture {
    static final int N = 32, OUTLINE = 0xFF30383E;
    /** A round part along a segment: half its width, its tones, whether its edge is outlined against the parts drawn before it (a flange), and whether its ends are cut square. */
    record Part(double x0, double y0, double x1, double y1, double half, int body, int light, int shade, boolean ring, boolean square) {
    }

    public static void main(String[] args) throws Exception {
        int steel = 0xFF929FA6, steelLight = 0xFFD7E1E6, steelShade = 0xFF6E7A82, grip = 0xFFD9822B, gripLight = 0xFFF2B45A, gripShade = 0xFFA85E1A;
        int flange = 0xFF7D8990, flangeLight = 0xFFB8C4CA, flangeShade = 0xFF5A656B;
        Part[] parts = {
                new Part(7.5, 24.5, 19.5, 12.5, 1.5, steel, steelLight, steelShade, false, false),     // shaft
                new Part(3.5, 28.5, 10, 22, 2.6, grip, gripLight, gripShade, false, false),             // grip
                new Part(15.5, 6.5, 25.5, 16.5, 2.0, steel, steelLight, steelShade, false, true),             // pipe spool across the tip
                new Part(14.2, 5.2, 15.9, 6.9, 3.4, flange, flangeLight, flangeShade, true, true),        // its flanges
                new Part(25.1, 16.1, 26.8, 17.8, 3.4, flange, flangeLight, flangeShade, true, true),
        };
        var image = new BufferedImage(N, N, BufferedImage.TYPE_INT_ARGB);
        var filled = new boolean[N][N];
        var owner = new int[N][N];
        for (int i = 0; i < parts.length; i++) {
            var part = parts[i];
            for (int y = 0; y < N; y++)
                for (int x = 0; x < N; x++) {
                    double s = side(part, x + .5, y + .5);
                    if (Double.isNaN(s)) continue;
                    filled[y][x] = true;
                    owner[y][x] = i;
                    image.setRGB(x, y, s > .15 * part.half && s < .65 * part.half ? part.light : s < -.45 * part.half ? part.shade : part.body);
                }
        }
        // A flange shows its edge against the pipe it sits on.
        var outlined = new boolean[N][N];
        for (int y = 0; y < N; y++)
            for (int x = 0; x < N; x++)
                if (filled[y][x] && !parts[owner[y][x]].ring)
                    for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}})
                        if (at(filled, x + d[0], y + d[1]) && parts[owner[y + d[1]][x + d[0]]].ring) outlined[y][x] = true;
        for (int y = 0; y < N; y++)
            for (int x = 0; x < N; x++)
                if (outlined[y][x]) image.setRGB(x, y, OUTLINE);
        for (int y = 0; y < N; y++)
            for (int x = 0; x < N; x++)
                if (!filled[y][x] && (at(filled, x - 1, y) || at(filled, x + 1, y) || at(filled, x, y - 1) || at(filled, x, y + 1))) image.setRGB(x, y, OUTLINE);
        ImageIO.write(image, "png", new File(args[0]));
        if (args.length > 1) {
            int scale = 8;
            var preview = new BufferedImage(N * scale, N * scale, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < N * scale; y++)
                for (int x = 0; x < N * scale; x++) {
                    int p = image.getRGB(x / scale, y / scale);
                    preview.setRGB(x, y, p >>> 24 == 0 ? ((x / 16 + y / 16) % 2 == 0 ? 0xC8C8C8 : 0xE8E8E8) : p & 0xFFFFFF);
                }
            ImageIO.write(preview, "png", new File(args[1]));
        }
    }

    /** Where (x, y) lies across the part, positive towards its upper-left side; NaN outside it. */
    static double side(Part p, double x, double y) {
        double dx = p.x1 - p.x0, dy = p.y1 - p.y0, length = Math.hypot(dx, dy);
        double ux = dx / length, uy = dy / length;
        double along = (x - p.x0) * ux + (y - p.y0) * uy;
        if (p.square && (along < 0 || along > length)) return Double.NaN;
        double cx = Math.max(0, Math.min(length, along));
        double ex = x - (p.x0 + ux * cx), ey = y - (p.y0 + uy * cx);
        if (Math.hypot(ex, ey) > p.half) return Double.NaN;
        // The normal pointing up and left (y grows downwards); for a part along the other diagonal, the one pointing up.
        double nx = uy, ny = -ux;
        if (nx + ny > 1e-9 || Math.abs(nx + ny) <= 1e-9 && ny > 0) {
            nx = -nx;
            ny = -ny;
        }
        return ex * nx + ey * ny;
    }

    static boolean at(boolean[][] filled, int x, int y) {
        return x >= 0 && y >= 0 && x < N && y < N && filled[y][x];
    }
}

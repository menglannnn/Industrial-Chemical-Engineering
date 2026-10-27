import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Deterministic pixel icon for the tall glass-and-pipe tank accessory. */
public final class LevelGaugeIcon {
    public static void main(String[] args)throws Exception{
        var image=new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
        g.setColor(new Color(0x30383d));g.fillRect(4,6,10,3);g.fillRect(4,24,10,3);
        g.setColor(new Color(0x879197));g.fillRect(4,6,8,1);g.fillRect(4,24,8,1);
        g.setColor(new Color(0x4c585f));g.fillRect(3,4,3,7);g.fillRect(3,22,3,7);
        g.setColor(new Color(0x737f86));g.fillRect(4,4,1,7);g.fillRect(4,22,1,7);
        g.setColor(new Color(0x343e44));g.fillRect(11,2,11,4);g.fillRect(11,27,11,4);
        g.setColor(new Color(0x8c989d));g.fillRect(12,2,9,1);g.fillRect(12,27,9,1);
        g.setColor(new Color(0x66737a));g.fillRect(12,6,2,21);g.fillRect(20,6,2,21);
        g.setColor(new Color(0x84999d, true));g.fillRect(14,6,6,21);
        g.setColor(new Color(0x879ca0));g.fillRect(14,6,6,9);
        g.setColor(new Color(0x6695a6));g.fillRect(14,15,6,12);
        g.setColor(new Color(0xc5e0e6));g.fillRect(14,7,1,19);g.fillRect(18,8,1,17);
        g.setColor(new Color(0x9d4938));g.fillRect(12,14,10,2);
        g.setColor(new Color(0xc4c9c4));g.fillRect(23,6,2,21);
        g.setColor(new Color(0x333a3d));for(int y=8;y<27;y+=4)g.fillRect(23,y,2,1);
        g.dispose();ImageIO.write(image,"png",Path.of(args[0]).toFile());
    }
}

import com.wormzjl.icheme.science.equipment.*;
import com.wormzjl.icheme.science.fluid.topology.TopologyCompiler.Kind;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

/** Flat software views of the actual production mesh; no Minecraft client or process solve. */
public final class ShutoffValveRender {
    private static final double[] EYE={.8,.65,1};
    public static void main(String[] args)throws Exception {
        var output=new File(args[0]);output.mkdirs();
        var image=new BufferedImage(1800,1040,BufferedImage.TYPE_INT_RGB);var g=graphics(image);
        title(g,"Pipeline shutoff valve | production geometry","Rows X / Y / Z; each diameter open then closed. Same world scale. Lever parallel = open, crosswise = closed.");
        int[] axes={5,1,3};double[] bores={.001,.05,1};
        for(int row=0;row<3;row++)for(int size=0;size<3;size++)for(int state=0;state<2;state++)
            scene(g,(size*2+state)*300,85+row*315,300,315,bores[size],axes[row],state==0,false,false,1.7,
                new String[]{"X","Y","Z"}[row]+" / "+bores[size]+" m / "+(state==0?"open":"closed"));
        save(image,g,new File(output,"axes-and-sizes.png"));
        image=new BufferedImage(1200,530,BufferedImage.TYPE_INT_RGB);g=graphics(image);
        title(g,"Pipeline shutoff valve | 0.25 m bore","Approved flanged ball-valve silhouette: split casting, bonnet, bolted joints and safety-yellow quarter-turn lever.");
        scene(g,0,85,600,440,.25,5,true,false,false,1.25,"Open / parallel lever");
        scene(g,600,85,600,440,.25,5,false,false,false,1.25,"Closed / crosswise lever");
        save(image,g,new File(output,"preview.png"));
        image=new BufferedImage(1200,480,BufferedImage.TYPE_INT_RGB);g=graphics(image);
        title(g,"Accessory and reducer","Ordinary pipe shoulders / joint seams retained. Accessory body and handle shown without the host pipe.");
        scene(g,0,85,400,390,.25,3,true,true,false,.65,"Accessory item / open");
        scene(g,400,85,400,390,.5,5,true,false,true,1.4,"0.5 m / reducer to 0.25 m");
        scene(g,800,85,400,390,1,1,false,false,false,1.7,"1 m vertical / intact handle");
        save(image,g,new File(output,"details.png"));
    }
    private static Graphics2D graphics(BufferedImage image){var g=image.createGraphics();g.setColor(new Color(236,241,245));g.fillRect(0,0,image.getWidth(),image.getHeight());return g;}
    private static void title(Graphics2D g,String title,String note){g.setColor(new Color(28,44,56));g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,25));g.drawString(title,22,34);g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,15));g.drawString(note,22,61);}
    private static void scene(Graphics2D g,int x,int y,int width,int height,double bore,int facing,boolean open,boolean accessory,boolean reducer,double span,String label) {
        g.setColor(Color.WHITE);g.fillRoundRect(x+7,y+5,width-14,height-10,12,12);
        g.setColor(new Color(27,44,56));g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,14));g.drawString(label,x+16,y+27);
        int code=PipeSizes.code(PipeSizes.outside(bore)),mask=3<<(facing&~1);long faces=0;
        for(int face=0;face<6;face++)if((mask>>face&1)!=0)faces|=(long)(reducer&&face==4?PipeSizes.code(PipeSizes.outside(.25)):code)<<(7*face);
        var key=new LineGeometry.Key(Kind.PIPE,facing,mask,code,faces,mask,false);
        var polygons=new ArrayList<>(ShutoffValveGeometry.polygons(key,open,accessory));
        double[] forward=unit(facing<2?new double[]{.8,.65,-1}:EYE),right=unit(new double[]{forward[2],0,-forward[0]}),up=cross(forward,right);
        polygons.sort(Comparator.comparingDouble(p->depth(p,forward)));
        double scale=Math.min(width-35,height-55)/span,ox=x+width/2.0,oy=y+height/2.0+35;
        for(var polygon:polygons) {
            var p=polygon.positions();double[] normal=cross(new double[]{p[3]-p[0],p[4]-p[1],p[5]-p[2]},new double[]{p[6]-p[0],p[7]-p[1],p[8]-p[2]});
            if(dot(normal,forward)<=1e-10)continue;var path=new Path2D.Double();
            for(int i=0;i<polygon.size();i++){double[] v={p[3*i]-.5,p[3*i+1]-.5,p[3*i+2]-.5};double px=ox+scale*dot(v,right),py=oy-scale*dot(v,up);if(i==0)path.moveTo(px,py);else path.lineTo(px,py);}path.closePath();
            int rgb=switch(polygon.material()){case BOLT->0x485057;case GUARD->0xffc52b;case OPENING->0x283139;case PIPE->0x8d9da6;default->0xb9c5cd;};
            double shade=.45+.55*Math.max(0,dot(unit(normal),unit(new double[]{-.35,.7,1})));
            g.setColor(new Color((int)((rgb>>16&255)*shade),(int)((rgb>>8&255)*shade),(int)((rgb&255)*shade)));g.fill(path);
        }
        System.out.printf(Locale.ROOT,"bore=%.3f axis=%d open=%s accessory=%s polygons=%d%n",bore,facing,open,accessory,polygons.size());
    }
    private static double depth(Mesh.Polygon p,double[] axis){double d=0;for(int i=0;i<p.size();i++)d+=p.positions()[3*i]*axis[0]+p.positions()[3*i+1]*axis[1]+p.positions()[3*i+2]*axis[2];return d/p.size();}
    private static double dot(double[] a,double[] b){return a[0]*b[0]+a[1]*b[1]+a[2]*b[2];}
    private static double[] cross(double[] a,double[] b){return new double[]{a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]};}
    private static double[] unit(double[] a){double n=Math.sqrt(dot(a,a));return new double[]{a[0]/n,a[1]/n,a[2]/n};}
    private static void save(BufferedImage image,Graphics2D g,File file)throws Exception{g.dispose();ImageIO.write(image,"png",file);System.out.println(file);}
}

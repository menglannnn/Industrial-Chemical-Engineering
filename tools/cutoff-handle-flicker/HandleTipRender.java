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
public final class HandleTipRender {
    private static final double[] EYE={.8,.65,1};
    private static int mode;
    private static boolean tipOnly;
    public static void main(String[] args)throws Exception {
        var output=new File(args[0]);output.mkdirs();
        var image=new BufferedImage(1200,650,BufferedImage.TYPE_INT_RGB);var g=graphics(image);
        title(g,"Cutoff lever tip | actual production geometry","Shaft and yellow grip finish inside their covers. Only the dark end cap closes the tip.");
        tipOnly=true;mode=0;scene(g,0,85,600,550,.5,3,true,false,false,.42,"Before: three coincident tip layers");
        mode=1;scene(g,600,85,600,550,.5,3,true,false,false,.42,"After: one cap face, unchanged silhouette");
        save(image,g,new File(output,"handle-tip-preview.png"));
        tipOnly=false;image=new BufferedImage(1800,1040,BufferedImage.TYPE_INT_RGB);g=graphics(image);
        title(g,"Cutoff lever | repaired end faces","Rows X / Y / Z; open and closed at minimum, default and maximum bore. Heavy handle dimensions retained.");
        int[] axes={5,1,3};double[] bores={.001,.05,1};
        for(int row=0;row<3;row++)for(int size=0;size<3;size++)for(int state=0;state<2;state++)
            scene(g,(size*2+state)*300,85+row*315,300,315,bores[size],axes[row],state==0,false,false,2.0,new String[]{"X","Y","Z"}[row]+" / "+bores[size]+" m / "+(state==0?"open":"closed"));
        save(image,g,new File(output,"axes-and-sizes.png"));
    }
    private static Graphics2D graphics(BufferedImage image){var g=image.createGraphics();g.setColor(new Color(236,241,245));g.fillRect(0,0,image.getWidth(),image.getHeight());return g;}
    private static void title(Graphics2D g,String title,String note){g.setColor(new Color(28,44,56));g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,25));g.drawString(title,22,34);g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,15));g.drawString(note,22,61);}
    private static void scene(Graphics2D g,int x,int y,int width,int height,double bore,int facing,boolean open,boolean accessory,boolean reducer,double span,String label) {
        g.setColor(Color.WHITE);g.fillRoundRect(x+7,y+5,width-14,height-10,12,12);
        g.setColor(new Color(27,44,56));g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,14));g.drawString(label,x+16,y+27);
        int code=PipeSizes.code(PipeSizes.outside(bore)),mask=3<<(facing&~1);long faces=0;
        for(int face=0;face<6;face++)if((mask>>face&1)!=0)faces|=(long)(reducer&&face==4?PipeSizes.code(PipeSizes.outside(.25)):code)<<(7*face);
        var key=new LineGeometry.Key(Kind.PIPE,facing,mask,code,faces,mask,false);
        var polygons=new ArrayList<>(mode==0?FlickeringShutoffValveGeometry.polygons(key,open,accessory):ShutoffValveGeometry.polygons(key,open,accessory));
        double[] forward=unit(facing<2?new double[]{.8,.65,-1}:EYE),right=unit(new double[]{forward[2],0,-forward[0]}),up=cross(forward,right);
        double loX=1e9,hiX=-1e9,loY=1e9,hiY=-1e9,minY=1e9,maxY=-1e9;
        for(var polygon:polygons)for(int i=0;i<polygon.size();i++) {
            var p=polygon.positions();double[] v={p[3*i]-.5,p[3*i+1]-.5,p[3*i+2]-.5};
            for(double value:v)if(!Double.isFinite(value))throw new AssertionError("Non-finite mesh");
            loX=Math.min(loX,dot(v,right));hiX=Math.max(hiX,dot(v,right));loY=Math.min(loY,dot(v,up));hiY=Math.max(hiY,dot(v,up));minY=Math.min(minY,p[3*i+1]);maxY=Math.max(maxY,p[3*i+1]);
        }
        if(tipOnly) {
            double od=PipeSizes.outsideOf(code),r=Math.min(.495,Math.max(.065,.64*od)),rise=Math.max(.055,.17*od),length=Math.min(.95,Math.max(.36,1.10*od));
            double[] stem=facing<2?new double[]{0,0,-1}:new double[]{0,1,0};var run=LineGeometry.STEPS[facing|1];double[] axis={run[0],run[1],run[2]};var lever=open?axis:cross(stem,axis);
            double[] focus={.97*length*lever[0]+(r+rise)*stem[0],.97*length*lever[1]+(r+rise)*stem[1],.97*length*lever[2]+(r+rise)*stem[2]};
            loX=dot(focus,right)-span/2;hiX=loX+span;loY=dot(focus,up)-span/2;hiY=loY+span;
        }
        double scale=Math.min(width-35,height-60)/Math.max(span,Math.max(hiX-loX,hiY-loY)*1.1),ox=width/2.0-scale*(loX+hiX)/2,oy=height/2.0+22+scale*(loY+hiY)/2;
        var pixels=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);var depths=new double[width*height];Arrays.fill(depths,Double.NEGATIVE_INFINITY);
        for(var polygon:polygons) {
            var p=polygon.positions();double[] normal=cross(new double[]{p[3]-p[0],p[4]-p[1],p[5]-p[2]},new double[]{p[6]-p[0],p[7]-p[1],p[8]-p[2]});
            if(dot(normal,forward)<=1e-10)continue;
            int rgb=switch(polygon.material()){case BOLT->0x485057;case GUARD->0xffc52b;case OPENING->0x283139;case PIPE->0x8d9da6;case ACTUATOR->0x577c70;case MACHINE->0x637c89;case STEELWORK->0x636d75;default->0xb9c5cd;};
            double shade=.45+.55*Math.max(0,dot(unit(normal),unit(new double[]{-.35,.7,1})));
            rgb=0xff000000|((int)((rgb>>16&255)*shade)<<16)|((int)((rgb>>8&255)*shade)<<8)|(int)((rgb&255)*shade);
            double[][] projected=new double[polygon.size()][3];
            for(int i=0;i<polygon.size();i++){double[] v={p[3*i]-.5,p[3*i+1]-.5,p[3*i+2]-.5};projected[i]=new double[]{ox+scale*dot(v,right),oy-scale*dot(v,up),dot(v,forward)};}
            for(int i=1;i+1<projected.length;i++)triangle(pixels,depths,projected[0],projected[i],projected[i+1],rgb);
        }
        g.drawImage(pixels,x,y,null);
        System.out.printf(Locale.ROOT,"mode=%d bore=%.3f facing=%d accessory=%s polygons=%d y=[%.4f,%.4f]%n",mode,bore,facing,accessory,polygons.size(),minY,maxY);
    }
    private static void triangle(BufferedImage image,double[] depth,double[] a,double[] b,double[] c,int rgb) {
        double area=edge(a,b,c[0],c[1]);if(Math.abs(area)<1e-10)return;
        int w=image.getWidth(),h=image.getHeight();
        int x0=Math.max(0,(int)Math.floor(Math.min(a[0],Math.min(b[0],c[0])))),x1=Math.min(w-1,(int)Math.ceil(Math.max(a[0],Math.max(b[0],c[0]))));
        int y0=Math.max(0,(int)Math.floor(Math.min(a[1],Math.min(b[1],c[1])))),y1=Math.min(h-1,(int)Math.ceil(Math.max(a[1],Math.max(b[1],c[1]))));
        for(int y=y0;y<=y1;y++)for(int x=x0;x<=x1;x++) {
            double u=edge(b,c,x+.5,y+.5)/area,v=edge(c,a,x+.5,y+.5)/area,t=1-u-v;
            if(u< -1e-8||v< -1e-8||t< -1e-8)continue;double z=u*a[2]+v*b[2]+t*c[2];int at=y*w+x;
            if(z>depth[at]){depth[at]=z;image.setRGB(x,y,rgb);}
        }
    }
    private static double edge(double[] a,double[] b,double x,double y){return (b[0]-a[0])*(y-a[1])-(b[1]-a[1])*(x-a[0]);}
    private static double dot(double[] a,double[] b){return a[0]*b[0]+a[1]*b[1]+a[2]*b[2];}
    private static double[] cross(double[] a,double[] b){return new double[]{a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]};}
    private static double[] unit(double[] a){double n=Math.sqrt(dot(a,a));return new double[]{a[0]/n,a[1]/n,a[2]/n};}
    private static void save(BufferedImage image,Graphics2D g,File file)throws Exception{g.dispose();ImageIO.write(image,"png",file);System.out.println(file);}
}

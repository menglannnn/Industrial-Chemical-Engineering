package com.wormzjl.icheme.runtime.fluid;

import com.wormzjl.icheme.science.equipment.*;
import com.wormzjl.icheme.science.fluid.topology.TopologyCompiler.Kind;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/** Offline views of the actual aperture, phase haze, bubble paths and rim colours. No Minecraft/world/solver. */
public final class FlowPreview {
    private record Face(double[] points,int colour,boolean shaded){}
    private static int base;
    private static double[] camera;
    private static SightGlassBubbles.Motion motion;
    public static void main(String[] args)throws Exception{
        Path output=Path.of(args[0]);Files.createDirectories(output.resolve("frames"));
        base=0xE88B8061;
        var axes=SightGlassGeometry.Axis.values();
        for(int frame=0;frame<56;frame++){
            var image=new BufferedImage(960,1080,BufferedImage.TYPE_INT_RGB);var g=graphics(image);
            for(int row=0;row<4;row++)for(int col=0;col<3;col++){
                var run=axes[row==3?0:row];var view=run==SightGlassGeometry.Axis.Z?SightGlassGeometry.Axis.X:row==3?SightGlassGeometry.Axis.Y:SightGlassGeometry.Axis.Z;
                boolean positive=col!=1,back=col==2;
                motion=SightGlassBubbles.Motion.along(view,run,positive);
                camera=switch(view){case X->new double[]{back?-1:1,.06,.03};case Y->new double[]{.03,back?-1:1,.06};case Z->new double[]{.03,.06,back?-1:1};};
                int mask=switch(run){case X->48;case Y->3;case Z->12;};
                String direction=switch(run){case X->positive?"East":"West";case Y->positive?"Up":"Down";case Z->positive?"South":"North";};
                scene(g,col*320,row*270,320,270,.65,mask,view,base,190,frame*2,direction+" / "+(back?"rear":"front")+" / window "+view);
            }
            g.dispose();ImageIO.write(image,"png",output.resolve(String.format("frames/frame-%03d.png",frame)).toFile());
        }
        System.out.println("56 frames x 12 native window/flow configurations; production bubble paths and aperture");
    }
    private static Graphics2D graphics(BufferedImage image){var g=image.createGraphics();g.setColor(Color.WHITE);g.fillRect(0,0,image.getWidth(),image.getHeight());g.setFont(new Font("Dialog",Font.PLAIN,16));return g;}
    private static void scene(Graphics2D g,int x,int y,int width,int height,double bore,int mask,SightGlassGeometry.Axis axis,int argb,int strength,double ticks,String label){
        g.setColor(new Color(0x20313B));g.drawString(label,x+12,y+23);
        for(int a=0;a<width;a+=18)for(int b=35;b<height;b+=18){g.setColor(new Color(((a/18+b/18)&1)==0?0xE0EAF0:0xF8FAFB));g.fillRect(x+a,y+b,Math.min(18,width-a),Math.min(18,height-b));}
        int face=Integer.numberOfTrailingZeros(mask),code=PipeSizes.code(PipeSizes.outside(bore));var key=new SightGlassGeometry.Key(new LineGeometry.Key(Kind.PIPE,face,mask,code,0,0,false),axis);
        var geometry=SightGlassGeometry.build(key,false);var faces=new ArrayList<Face>();
        for(var p:geometry.metal())faces.add(new Face(p.positions(),metal(p.material()),true));
        for(var p:geometry.glass())faces.add(new Face(p.positions(),0x28EAF5FF,false));
        if(argb>>>24>0)for(var p:geometry.liquid())faces.add(new Face(p.positions(),argb,false));
        var window=SightGlassGeometry.window(key);int rim=SightGlassBubbles.colour(argb,strength);
        for(int side:new int[]{1,-1})for(var bubble:SightGlassBubbles.at(ticks,9,strength,motion))for(int i=0;i<12;i++){
            double[] points=new double[12];
            for(int corner=0;corner<4;corner++){
                int angle=(corner==0||corner==3)?i:(i+1)%12;double a=2*Math.PI*angle/12,r=bubble.radius()*(corner<2?1:.72);
                double X=window.radius()*(side*bubble.x()+r*Math.cos(a)),Y=window.radius()*(bubble.y()+r*Math.sin(a));
                var p=point(axis,side,X,Y,window.plane());System.arraycopy(p,0,points,3*corner,3);
            }
            faces.add(new Face(points,rim,false));
        }
        double[] eye=camera!=null?unit(camera):switch(axis){case Z->unit(new double[]{.18,.12,1});case X->unit(new double[]{1,.12,.18});case Y->unit(new double[]{.18,1,.22});};
        double[] right=unit(new double[]{eye[2],0,-eye[0]}),up=cross(eye,right);
        faces.sort(Comparator.comparingDouble(f->depth(f.points(),eye)));double scale=Math.min(width-30,height-58),ox=x+width/2d,oy=y+height/2d+12;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_OFF);
        for(var faceValue:faces){var p=faceValue.points();var normal=cross(new double[]{p[3]-p[0],p[4]-p[1],p[5]-p[2]},new double[]{p[6]-p[0],p[7]-p[1],p[8]-p[2]});if(dot(normal,eye)<=1e-14)continue;
            var path=new Path2D.Double();for(int i=0;i<p.length;i+=3){double[] v={p[i]-.5,p[i+1]-.5,p[i+2]-.5};double a=ox+scale*dot(v,right),b=oy-scale*dot(v,up);if(i==0)path.moveTo(a,b);else path.lineTo(a,b);}path.closePath();
            int colour=faceValue.colour();double shade=faceValue.shaded()?.48+.52*Math.max(0,dot(unit(normal),unit(new double[]{-.35,.7,1}))):1;
            g.setColor(new Color((int)((colour>>16&255)*shade),(int)((colour>>8&255)*shade),(int)((colour&255)*shade),colour>>>24));g.fill(path);
        }
    }
    private static double[] point(SightGlassGeometry.Axis axis,int side,double x,double y,double plane){return switch(axis){case Z->new double[]{.5+side*x,.5+y,.5+side*plane};case X->new double[]{.5+side*plane,.5+y,.5-side*x};case Y->new double[]{.5+side*x,.5+side*plane,.5-y};};}
    private static int metal(Mesh.Material m){return switch(m){case BOLT->0xFF48545F;case FLANGE->0xFFA6B6C1;case OPENING->0xFF29333A;default->0xFFBECAD2;};}
    private static double depth(double[] p,double[] n){double sum=0;for(int i=0;i<p.length;i+=3)sum+=p[i]*n[0]+p[i+1]*n[1]+p[i+2]*n[2];return sum/(p.length/3);}
    private static double dot(double[] a,double[] b){return a[0]*b[0]+a[1]*b[1]+a[2]*b[2];}
    private static double[] cross(double[] a,double[] b){return new double[]{a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]};}
    private static double[] unit(double[] a){double n=Math.sqrt(dot(a,a));return new double[]{a[0]/n,a[1]/n,a[2]/n};}
}

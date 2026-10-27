package com.wormzjl.icheme.runtime.fluid;

import com.wormzjl.icheme.science.equipment.LineGeometry;
import com.wormzjl.icheme.science.equipment.Mesh;
import com.wormzjl.icheme.science.equipment.PipeSizes;
import com.wormzjl.icheme.science.equipment.SightGlassGeometry;
import com.wormzjl.icheme.science.fluid.topology.TopologyCompiler.Kind;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

/** Offline renders of production geometry and turbidity policy. Flat shading and example base colours, no game/world. */
public final class SightGlassRender {
    private record Face(Mesh.Polygon polygon,int colour){}
    private static final int BLUE=0x8092BED2,OIL=0xC03D653A;
    private static final double[] NORMAL={.20,.12,1},EAST={1,.12,.20},DIAGONAL={1,.12,1};
    public static void main(String[] args)throws Exception {
        var output=new File(args[0]);output.mkdirs();
        var views=new BufferedImage(1200,945,BufferedImage.TYPE_INT_RGB);var g=graphics(views);
        title(g,"Pipeline sight glass | actual mesh", "Straight X, Z and vertical runs. Clear, liquid and mixed-phase haze. Cast body and recessed four-bolt windows.");
        int[] masks={48,12,3};String[] labels={"Horizontal X | side windows","Horizontal Z | side windows","Vertical | N/S windows"};
        for(int row=0;row<3;row++)for(int col=0;col<3;col++) {
            int colour=col==0?0:SightGlassDisplay.turbid(BLUE,col==1?new double[]{0,1,0}:new double[]{0,.5,.5},0);
            scene(g,400*col,90+285*row,400,285,.25,masks[row],colour, row==1?EAST:NORMAL,
                    labels[row]+" / "+new String[]{"clear","liquid","turbid"}[col],false,false);
        }
        save(views,new File(output,"views.png"));g.dispose();

        var sizes=new BufferedImage(1500,1045,BufferedImage.TYPE_INT_RGB);g=graphics(sizes);
        title(g,"Pipe-diameter scaling | same image scale", "Bores 0.001, 0.05, 0.25, 0.5, 1 m. Clear run, liquid run, turbid vertical run. Existing drawing minimum/steps retained.");
        double[] bores={.001,.05,.25,.5,1};
        for(int row=0;row<3;row++)for(int col=0;col<5;col++)scene(g,300*col,90+315*row,300,315,bores[col],row==2?3:48,
                row==0?0:SightGlassDisplay.turbid(OIL,row==2?new double[]{.5,0,.5}:new double[]{1,0,0},0),NORMAL,
                "Bore "+bores[col]+" m / "+(row==0?"clear":row==1?"liquid":"turbid vertical"),false,false);
        save(sizes,new File(output,"sizes.png"));g.dispose();

        var details=new BufferedImage(1200,1020,BufferedImage.TYPE_INT_RGB);g=graphics(details);
        title(g,"Fittings, coverage and accessory", "Production mesh with representative display states. Window-cover bolts scale with the housing.");
        scene(g,0,90,400,300,.5,48,SightGlassDisplay.turbid(OIL,new double[]{.9,.1,0},0),NORMAL,"Reducer to 0.25 m neighbour",true,false);
        scene(g,400,90,400,300,.25,1|16,0,NORMAL,"Vertical corner / ordinary pipe",false,false);
        scene(g,800,90,400,300,.25,16|4,0,DIAGONAL,"Horizontal corner / ordinary pipe",false,false);
        scene(g,0,390,400,300,.25,48,0,NORMAL,"Accessory item / empty",false,true);
        scene(g,400,390,400,300,.25,48,SightGlassDisplay.turbid(OIL,new double[]{.99,0,0},.01),NORMAL,"Liquid + 1% solids",false,false);
        scene(g,800,390,400,300,.25,48,SightGlassDisplay.turbid(BLUE,new double[]{0,0,1},0),NORMAL,"Gas only / clear",false,false);
        scene(g,0,690,400,300,1,48,0,new double[]{.65,.4,1},"Body depth / 1 m / clear",false,false);
        scene(g,400,690,400,300,.25,48,SightGlassDisplay.turbid(BLUE,new double[]{.5,.5,0},0),new double[]{.65,.4,1},"Body depth / two liquids",false,false);
        scene(g,800,690,400,300,.25,63,0,DIAGONAL,"Junction / ordinary pipe only",false,false);
        save(details,new File(output,"details.png"));g.dispose();
        var preview=new BufferedImage(1000,480,BufferedImage.TYPE_INT_RGB);g=graphics(preview);
        title(g,"Revised pipeline sight glass", "Cast chamber, bevelled retaining covers, recessed round windows and four fasteners per side. Bore 0.25 m.");
        scene(g,0,90,500,380,.25,48,0,new double[]{.6,.3,1},"Clear / no passing liquid",false,false);
        scene(g,500,90,500,380,.25,48,SightGlassDisplay.turbid(BLUE,new double[]{0,1,0},0),new double[]{.6,.3,1},"Passing liquid / representative colour",false,false);
        save(preview,new File(output,"preview.png"));g.dispose();
        var orientations=new BufferedImage(1200,1020,BufferedImage.TYPE_INT_RGB);g=graphics(orientations);
        title(g,"Installed towards the player | actual mesh", "Rows: X, Y, Z pipe runs. Installer at side, alternate side/above, and opposite side. Windows stay perpendicular.");
        int[] runMasks={48,3,12};double[][][] installers={{{0,.2,3},{0,3,.2},{0,.2,-3}},{{.2,0,3},{3,0,.2},{-3,0,.2}},{{3,.2,0},{.2,3,0},{-3,.2,0}}};
        for(int row=0;row<3;row++)for(int col=0;col<3;col++) {
            var eye=installers[row][col];var axis=SightGlassGeometry.towards(runMasks[row],5,eye[0],eye[1],eye[2]);
            double[] camera=eye.clone();for(int a=0;a<3;a++)camera[a]+=.2;
            scene(g,400*col,90+300*row,400,300,.25,runMasks[row],0,camera,
                    new String[]{"X","Y","Z"}[row]+" run / window "+axis+" / installer "+col,false,false,axis);
            System.out.printf("installer run=%d offset=%s selected=%s%n",runMasks[row],Arrays.toString(eye),axis);
        }
        save(orientations,new File(output,"orientations.png"));g.dispose();
        System.out.printf("single liquid=%08X, 50/50 liquid-gas=%08X, gas only=%08X%n",SightGlassDisplay.turbid(BLUE,new double[]{0,1,0},0),SightGlassDisplay.turbid(BLUE,new double[]{0,.5,.5},0),SightGlassDisplay.turbid(BLUE,new double[]{0,0,1},0));
    }
    private static Graphics2D graphics(BufferedImage image){var g=image.createGraphics();g.setColor(new Color(237,242,246));g.fillRect(0,0,image.getWidth(),image.getHeight());g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);return g;}
    private static void title(Graphics2D g,String title,String subtitle){g.setColor(new Color(26,44,57));g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,25));g.drawString(title,24,35);g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,15));g.drawString(subtitle,24,63);}
    private static void scene(Graphics2D g,int x,int y,int width,int height,double bore,int mask,int argb,double[] eye,String label,boolean reducer,boolean accessory) {
        scene(g,x,y,width,height,bore,mask,argb,eye,label,reducer,accessory,null);
    }
    private static void scene(Graphics2D g,int x,int y,int width,int height,double bore,int mask,int argb,double[] eye,String label,boolean reducer,boolean accessory,SightGlassGeometry.Axis axis) {
        g.setColor(Color.WHITE);g.fillRoundRect(x+8,y+5,width-16,height-10,12,12);
        for(int cx=x+10;cx<x+width-10;cx+=16)for(int cy=y+42;cy<y+height-18;cy+=16)if(((cx-x)/16+(cy-y)/16)%2==0){g.setColor(new Color(226,237,242));g.fillRect(cx,cy,16,16);}
        g.setColor(new Color(27,47,61));g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,14));g.drawString(label,x+17,y+27);
        int code=PipeSizes.code(PipeSizes.outside(bore));long faces=0;
        for(int face=0;face<6;face++)if((mask>>face&1)!=0)faces|=(long)(reducer&&face==4?PipeSizes.code(PipeSizes.outside(.25)):code)<<(7*face);
        var key=new LineGeometry.Key(Kind.PIPE,mask==3?1:mask==12?3:5,mask,code,faces,mask,false);
        var look=axis==null?new SightGlassGeometry.Key(key):new SightGlassGeometry.Key(key,axis);
        var mesh=SightGlassGeometry.build(look,accessory);var list=new ArrayList<Face>();
        for(var p:mesh.metal())list.add(new Face(p,metal(p.material())));
        for(var p:mesh.glass())list.add(new Face(p,0x24C2E5F3));
        if(argb>>>24>0)for(var p:mesh.liquid())list.add(new Face(p,argb));
        double[] forward=unit(eye),right=unit(new double[]{forward[2],0,-forward[0]}),up=cross(forward,right);
        list.sort(Comparator.comparingDouble(f->depth(f.polygon,forward)));
        double scale=Math.min(width-46,height-62),ox=x+width/2.0,oy=y+height/2.0+13;
        // Adjacent faces share exact edges. Antialiasing each separately leaves artificial hairline gaps between them.
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_OFF);
        for(var face:list) {
            var p=face.polygon.positions();double[] a={p[3]-p[0],p[4]-p[1],p[5]-p[2]},b={p[6]-p[0],p[7]-p[1],p[8]-p[2]},normal=cross(a,b);
            if(dot(normal,forward)<=1e-10)continue;
            var path=new Path2D.Double();
            for(int corner=0;corner<face.polygon.size();corner++){double[] point={p[3*corner]-.5,p[3*corner+1]-.5,p[3*corner+2]-.5};double px=ox+scale*dot(point,right),py=oy-scale*dot(point,up);if(corner==0)path.moveTo(px,py);else path.lineTo(px,py);}
            path.closePath();int colour=face.colour;
            double shade=.45+.55*Math.max(0,dot(unit(normal),unit(new double[]{-.35,.7,1})));
            if(face.polygon.material()==Mesh.Material.GLASS)shade=1;
            g.setColor(new Color((int)((colour>>16&255)*shade),(int)((colour>>8&255)*shade),(int)((colour&255)*shade),colour>>>24));g.fill(path);
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        System.out.printf("bore=%.3f OD=%.5f mask=%d accessory=%s metal=%d glass=%d liquid=%d%n",bore,PipeSizes.outsideOf(code),mask,accessory,mesh.metal().size(),mesh.glass().size(),mesh.liquid().size());
    }
    private static int metal(Mesh.Material m){return switch(m){case BOLT->0xFF48545F;case FLANGE->0xFFA6B6C1;case OPENING->0xFF29333A;default->0xFFBECAD2;};}
    private static double depth(Mesh.Polygon polygon,double[] axis){double d=0;for(int i=0;i<polygon.size();i++)d+=polygon.positions()[3*i]*axis[0]+polygon.positions()[3*i+1]*axis[1]+polygon.positions()[3*i+2]*axis[2];return d/polygon.size();}
    private static double dot(double[] a,double[] b){return a[0]*b[0]+a[1]*b[1]+a[2]*b[2];}
    private static double[] cross(double[] a,double[] b){return new double[]{a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]};}
    private static double[] unit(double[] a){double n=Math.sqrt(dot(a,a));return new double[]{a[0]/n,a[1]/n,a[2]/n};}
    private static void save(BufferedImage image,File file)throws Exception{ImageIO.write(image,"png",file);System.out.println(file);}
}

import com.google.gson.*;
import com.wormzjl.icheme.science.equipment.*;
import com.wormzjl.icheme.science.fluid.topology.TopologyCompiler.Kind;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;

/** Actual item meshes and JSON GUI transforms, drawn in a 16px slot without Minecraft. */
public final class AccessoryIconRender {
    record Pose(double[] rotation,double[] translation,double[] scale){}
    record Face(double[] points,Mesh.Material material,double z){}
    public static void main(String[] args)throws Exception {
        Path root=Path.of(args[0]),out=Path.of(args[1]);Files.createDirectories(out);
        Pose before;try(var zip=new ZipFile(args[2])){before=pose(JsonParser.parseString(new String(zip.getInputStream(zip.getEntry("assets/minecraft/models/block/block.json")).readAllBytes())).getAsJsonObject());}
        var sheet=new BufferedImage(760,390,BufferedImage.TYPE_INT_RGB);var g=sheet.createGraphics();g.setColor(new Color(32,42,49));g.fillRect(0,0,760,390);g.setColor(Color.WHITE);g.setFont(new Font("SansSerif",Font.BOLD,22));g.drawString("Accessory inventory icons | actual 16px slot bounds",18,30);
        for(int item=0;item<2;item++) {
            String id=item==0?"pipeline_sight_glass":"pipeline_shutoff_valve";
            var json=JsonParser.parseString(Files.readString(root.resolve("src/main/resources/assets/icheme/models/item/"+id+".json"))).getAsJsonObject();
            var key=LineGeometry.Key.item(Kind.PIPE);var mesh=new ArrayList<Mesh.Polygon>();
            if(item==0){var glass=SightGlassGeometry.build(new SightGlassGeometry.Key(key),true);mesh.addAll(glass.metal());mesh.addAll(glass.glass());}else mesh.addAll(ShutoffValveGeometry.polygons(key,true,true));
            for(int pass=0;pass<2;pass++) {
                Pose p=pass==0?before:pose(json);int x=20+pass*380,y=55+item*165;
                var icon=icon(mesh,p,id,pass==1);g.setColor(Color.WHITE);g.setFont(new Font("SansSerif",Font.PLAIN,16));g.drawString((item==0?"Sight glass":"Shutoff valve")+" / "+(pass==0?"before":"after"),x,y+18);
                g.drawImage(icon,x,y+30,128,128,null);g.drawImage(icon,x+150,y+78,null);
                g.drawString("scale "+p.scale[0],x+180,y+91);
                ImageIO.write(icon,"png",out.resolve(id+(pass==0?"-before":"-after")+".png").toFile());
            }
        }
        g.dispose();ImageIO.write(sheet,"png",out.resolve("accessory-icons.png").toFile());
    }
    private static Pose pose(JsonObject model){var gui=model.getAsJsonObject("display").getAsJsonObject("gui");return new Pose(array(gui,"rotation"),array(gui,"translation"),array(gui,"scale"));}
    private static double[] array(JsonObject json,String key){var a=json.getAsJsonArray(key);return new double[]{a.get(0).getAsDouble(),a.get(1).getAsDouble(),a.get(2).getAsDouble()};}
    private static double[] at(double[] a,int v,Pose p){double x=(a[v]-.5)*p.scale[0],y=(a[v+1]-.5)*p.scale[1],z=(a[v+2]-.5)*p.scale[2];double rx=Math.toRadians(p.rotation[0]),ry=Math.toRadians(p.rotation[1]),rz=Math.toRadians(p.rotation[2]);double xx=Math.cos(rz)*x-Math.sin(rz)*y; y=Math.sin(rz)*x+Math.cos(rz)*y;x=xx;xx=Math.cos(ry)*x+Math.sin(ry)*z;z=-Math.sin(ry)*x+Math.cos(ry)*z;x=xx;double yy=Math.cos(rx)*y-Math.sin(rx)*z;z=Math.sin(rx)*y+Math.cos(rx)*z;return new double[]{8+16*x+p.translation[0],8-16*yy-p.translation[1],z};}
    private static BufferedImage icon(List<Mesh.Polygon> mesh,Pose pose,String id,boolean enlarged){
        var image=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();g.setColor(new Color(58,63,56));g.fillRect(0,0,16,16);var faces=new ArrayList<Face>();double minX=99,maxX=-99,minY=99,maxY=-99;
        for(var p:mesh){var a=p.positions();var xy=new double[a.length];double depth=0;for(int i=0;i<a.length;i+=3){var v=at(a,i,pose);System.arraycopy(v,0,xy,i,3);minX=Math.min(minX,v[0]);maxX=Math.max(maxX,v[0]);minY=Math.min(minY,v[1]);maxY=Math.max(maxY,v[1]);depth+=v[2];}faces.add(new Face(xy,p.material(),depth/p.size()));}
        faces.sort(Comparator.comparingDouble(Face::z));
        for(var f:faces){var p=f.points;double area=(p[3]-p[0])*(p[7]-p[1])-(p[4]-p[1])*(p[6]-p[0]);if(area>=0)continue;var path=new Path2D.Double();for(int i=0;i<p.length;i+=3){if(i==0)path.moveTo(p[i],p[i+1]);else path.lineTo(p[i],p[i+1]);}path.closePath();int rgb=switch(f.material){case BOLT->0x495159;case GUARD->0xffc52b;case OPENING->0x252b2e;case GLASS->0x809ba3;default->0xa4afb7;};double nx=(p[4]-p[1])*(p[8]-p[2])-(p[5]-p[2])*(p[7]-p[1]);double ny=(p[5]-p[2])*(p[6]-p[0])-(p[3]-p[0])*(p[8]-p[2]);double nz=area;double shade=.6+.4*Math.max(0,-(.3*nx+.7*ny+.6*nz)/Math.sqrt(nx*nx+ny*ny+nz*nz));g.setColor(new Color((int)((rgb>>16&255)*shade),(int)((rgb>>8&255)*shade),(int)((rgb&255)*shade),f.material==Mesh.Material.GLASS?90:255));g.fill(path);}
        g.dispose();System.out.printf(Locale.ROOT,"%s enlarged=%s bounds=(%.3f,%.3f)..(%.3f,%.3f) size=%.3fx%.3f px%n",id,enlarged,minX,minY,maxX,maxY,maxX-minX,maxY-minY);
        if(enlarged&&(minX<0||minY<0||maxX>16||maxY>16))throw new AssertionError("Icon clipped");return image;
    }
}

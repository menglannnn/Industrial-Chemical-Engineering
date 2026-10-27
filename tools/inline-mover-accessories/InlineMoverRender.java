package com.wormzjl.icheme.science.equipment;

import com.wormzjl.icheme.science.equipment.EquipmentShape.Heading;
import com.wormzjl.icheme.science.equipment.EquipmentShape.Kind;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Offline textured native-mesh sweep for inline movers: four headings, three bores, three full views,
 * lower-assembly closeups and item looks. Backface culling follows the chunk mesh; ground follows
 * the lowest overhang. Run from the compiled worktree; args: output-directory. */
public class InlineMoverRender {
    private static final String ASSETS = "src/main/resources/assets/icheme/";

    private record Triangle(double[][] p, double[][] uv, double[] normal, Mesh.Material material, int colour) {
    }

    private record Sprite(int width, int height, int[] argb) {
        int sample(double u, double v) {
            int x = Math.floorMod((int) Math.floor(u * width), width), y = Math.floorMod((int) Math.floor(v * height), height);
            return argb[y * width + x];
        }
    }

    private static double[] focus;
    private static double zoom=0;
    private static double frame=1;
    private static double probeDepth=.12;
    private static double cropTop=Double.POSITIVE_INFINITY;
    public static void main(String[] args)throws Exception{
        String root=args[0];Files.createDirectories(Path.of(root));var sprites=sprites();int renders=0;
        var dummy=EquipmentShape.drum(2,4,EquipmentShape.Orientation.VERTICAL,Heading.NORTH,0);
        for(boolean compressor:new boolean[]{false,true})for(int facing:new int[]{2,3,4,5})for(double bore:new double[]{.001,.05,.15,.2,.21,.4,1})for(int view=0;view<3;view++){
            var key=new LineGeometry.Key(com.wormzjl.icheme.science.fluid.topology.TopologyCompiler.Kind.PIPE,facing,3<<(facing&~1),PipeSizes.code(PipeSizes.outside(bore)),0,0,false);
            var triangles=new ArrayList<Triangle>();var polygons=InlineMoverGeometry.polygons(key,compressor,false);
            for(var p:polygons){for(double x:p.positions())if(!Double.isFinite(x))throw new AssertionError("Nonfinite model");add(triangles,p,0,0,0);}
            focus=new double[]{.5,.5,.5};zoom=0;var direction=new double[][]{{-.65,-.25,1},{.65,-.18,-1},{1,.12,-.25}}[view];
            render(triangles,dummy,unit(direction),sprites,root+"/"+(compressor?"compressor":"pump")+"-"+facing+"-"+bore+"-"+view+".png");renders++;
            if(view==0){double od=PipeSizes.outsideOf(key.outside()),r=Math.max(compressor?.15:.12,(compressor?.78:.72)*od+.04);cropTop=.5+(compressor?Math.max(.57*r,.53*od)+.025+.035*od+1.35*r+.18:Math.max(.57*r,.53*od)+.025)+.035;
                render(triangles,dummy,unit(direction),sprites,root+"/"+(compressor?"compressor":"pump")+"-"+facing+"-"+bore+"-lower.png");renders++;cropTop=Double.POSITIVE_INFINITY;}
            if(view==0&&facing==2){
                var connected=new ArrayList<>(triangles);double od=PipeSizes.outsideOf(key.outside());
                for(int face:new int[]{2,3}){
                    var stub=new Mesh();var end=InlineMoverGeometry.boundaryJoints(key.outside())?LineGeometry.End.flanged(od,true):new LineGeometry.End(od,false,0,0,true);
                    LineGeometry.arm(stub,face^1,.20,od,end,LineGeometry.ref(face^1),PipeSizes.sides(od),false);
                    var offset=LineGeometry.step(face);for(var p:LineGeometry.overhanging(stub))add(connected,p,(int)offset[0],(int)offset[1],(int)offset[2]);
                }
                render(connected,dummy,unit(new double[]{1,-.22,.35}),sprites,root+"/"+(compressor?"compressor":"pump")+"-2-"+bore+"-connected.png");renders++;
            }

        }
        for(boolean compressor:new boolean[]{false,true}){
            var key=LineGeometry.Key.item(com.wormzjl.icheme.science.fluid.topology.TopologyCompiler.Kind.PUMP);var triangles=new ArrayList<Triangle>();for(var p:InlineMoverGeometry.polygons(key,compressor,true))add(triangles,p,0,0,0);
            render(triangles,dummy,unit(new double[]{-.65,-.25,1}),sprites,root+"/"+(compressor?"compressor":"pump")+"-item.png");renders++;
            double od=PipeSizes.outsideOf(key.outside()),r=Math.max(compressor?.15:.12,(compressor?.78:.72)*od+.04);
            cropTop=.5+Math.max(.57*r,.53*od)+.06;
            render(triangles,dummy,unit(new double[]{-.65,-.25,1}),sprites,root+"/"+(compressor?"compressor":"pump")+"-item-lower.png");renders++;cropTop=Double.POSITIVE_INFINITY;
        }
        System.out.println("Native software renders="+renders+"; finite geometry across horizontal headings/diameter extremes");
    }
    private static void gauge(List<Triangle> out,double[] centre,Heading face,double ratio,int tint){gauge(out,centre,face,ratio,tint,true);}
    private static void gauge(List<Triangle> out,double[] centre,Heading face,double ratio,int tint,boolean pressure){var quads=new ArrayList<>(InstrumentGeometry.housing());quads.add(InstrumentGeometry.needle(ratio));for(var q:quads){if(tint==0x38ad7770)continue;var v=q.vertices();double[] p=new double[12];for(int i=0;i<4;i++){double x=v[3*i],y=v[3*i+1],z=v[3*i+2];p[3*i]=centre[0]+face.dz*x+face.dx*z;p[3*i+1]=centre[1]+y;p[3*i+2]=centre[2]-face.dx*x+face.dz*z;}add(out,new Mesh.Polygon(p,new double[8],new double[12],Mesh.Material.FLANGE),0,0,0,tint==0?q.colour():((tint&0xff000000)|(q.colour()&0x00ffffff)));}
        if(tint!=0){for(int i=0;i<32;i++){double a=2*Math.PI*i/32,b=2*Math.PI*(i+1)/32;double[] local={.176*Math.cos(a),.176*Math.sin(a),.073,.178*Math.cos(a),.178*Math.sin(a),.073,.178*Math.cos(b),.178*Math.sin(b),.073,.176*Math.cos(b),.176*Math.sin(b),.073};double[] p=new double[12];for(int j=0;j<4;j++){p[3*j]=centre[0]+face.dz*local[3*j]+face.dx*local[3*j+2];p[3*j+1]=centre[1]+local[3*j+1];p[3*j+2]=centre[2]-face.dx*local[3*j]+face.dz*local[3*j+2];}add(out,new Mesh.Polygon(p,new double[8],new double[12],Mesh.Material.FLANGE),0,0,0,0x87000000|(tint&0xffffff));}return;}
        label(out,centre,face,pressure?"bar(g)":"°C",0,-.098,.0025);
        for(int i=0;i<=4;i++){double f=i/4d,a=Math.PI*(1.25-1.5*f);label(out,centre,face,String.format(java.util.Locale.ROOT,"%.1f",f*(pressure?10:200)),.081*Math.cos(a),.081*Math.sin(a)+.010,.0023);}
    }
    private static void label(List<Triangle> out,double[] c,Heading face,String text,double x,double y,double scale){var image=new BufferedImage(100,20,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();g.setFont(new java.awt.Font("Dialog",java.awt.Font.PLAIN,10));int width=g.getFontMetrics().stringWidth(text);g.setColor(java.awt.Color.BLACK);g.drawString(text,0,10);g.dispose();for(int row=0;row<14;row++)for(int col=0;col<width;col++)if(image.getRGB(col,row)>>>24>128){double a=x+(col-width/2d)*scale,b=y-row*scale;double[] v={a,b,.050,a+scale,b,.050,a+scale,b-scale,.050,a,b-scale,.050};double[] p=new double[12];for(int i=0;i<4;i++){p[3*i]=c[0]+face.dz*v[3*i]+face.dx*v[3*i+2];p[3*i+1]=c[1]+v[3*i+1];p[3*i+2]=c[2]-face.dx*v[3*i]+face.dz*v[3*i+2];}double[] rev={p[9],p[10],p[11],p[6],p[7],p[8],p[3],p[4],p[5],p[0],p[1],p[2]};add(out,new Mesh.Polygon(rev,new double[8],new double[12],Mesh.Material.FLANGE),0,0,0,0xff252b2e);}}
    private static void gaugeConnection(List<Triangle> out,double[] c,Heading h,double ratio,boolean temperature,double[] tap,boolean bottom){
        gauge(out,c,h,ratio,0,!temperature);
        for(var q:InstrumentGeometry.connection(tap,bottom,temperature,probeDepth)){
            var v=q.vertices();var p=new double[12];for(int i=0;i<4;i++){p[3*i]=c[0]+h.dz*v[3*i]+h.dx*v[3*i+2];p[3*i+1]=c[1]+v[3*i+1];p[3*i+2]=c[2]-h.dx*v[3*i]+h.dz*v[3*i+2];}
            add(out,new Mesh.Polygon(p,new double[8],new double[12],Mesh.Material.FLANGE),0,0,0,q.colour());
        }
    }
    private static void audit(){for(var q:InstrumentGeometry.housing())for(double v:q.vertices())if(!Double.isFinite(v))throw new AssertionError("nonfinite housing");if(!(InstrumentGeometry.FACE<InstrumentGeometry.TICKS&&InstrumentGeometry.TICKS<InstrumentGeometry.LABEL&&InstrumentGeometry.LABEL<InstrumentGeometry.NEEDLE))throw new AssertionError("overlapping layers");System.out.println("Closed housing, connector and all-axis filter geometry: finite; separated layers");}
    /** Same cached product box faces/scale used by TankRenderer; baseline reproduces its former winding verbatim. */
    private static void band(List<Triangle> out,ShapeRasterizer.Floating f,double lo,double hi,int colour,double half,boolean baseline) {
        if(hi<=lo)return;var faces=new ArrayList<Mesh.Polygon>();
        if(!baseline)faces.addAll(TankGaugeGeometry.bandFaces());
        else {
            double[][] sides={{0,0,1,0},{1,0,1,1},{1,1,0,1},{0,1,0,0}};
            for(var q:sides)faces.add(new Mesh.Polygon(new double[]{q[0],0,q[1],q[2],0,q[3],q[2],1,q[3],q[0],1,q[1]},new double[8],new double[12],Mesh.Material.OPENING));
            faces.add(new Mesh.Polygon(new double[]{0,1,0,0,1,1,1,1,1,1,1,0},new double[8],new double[12],Mesh.Material.OPENING));
        }
        for(var face:faces){var p=face.positions().clone();for(int i=0;i<p.length;i+=3){p[i]=f.glass()[0]+half*(2*p[i]-1);p[i+1]=lo+(hi-lo)*p[i+1];p[i+2]=f.glass()[1]+half*(2*p[i+2]-1);}add(out,new Mesh.Polygon(p,face.uvs(),face.normals(),face.material()),0,0,0,colour);}
    }

    /** The sprites by material, from the shell model's texture names. */
    private static Map<Mesh.Material, Sprite> sprites() throws Exception {
        String json = Files.readString(Path.of(ASSETS + "models/block/equipment_shell.json"));
        var names = Pattern.compile("\"(\\w+)\"\\s*:\\s*\"icheme:block/(\\w+)\"").matcher(json);
        var files = new java.util.HashMap<String, String>();
        while (names.find()) files.put(names.group(1), names.group(2));
        var out = new EnumMap<Mesh.Material, Sprite>(Mesh.Material.class);
        for (var material : Mesh.Material.values()) {
            String file = files.get(material.texture());
            if (file == null) continue;
            var image = ImageIO.read(new File(ASSETS + "textures/block/" + file + ".png"));
            // An animated strip shows its first frame.
            int w = image.getWidth(), h = image.getHeight() > w && image.getHeight() % w == 0 ? w : image.getHeight();
            int[] argb = new int[w * h];
            image.getRGB(0, 0, w, h, argb, 0, w);
            out.put(material, new Sprite(w, h, argb));
        }
        out.put(Mesh.Material.GAUGE_SCALE,new Sprite(1,1,new int[]{0xffd5d6d1}));
        return out;
    }

    /** {@code v} turned by {@code angle} about the unit {@code axis} (Rodrigues). */
    private static double[] rotate(double[] v, double[] axis, double angle) {
        double c = Math.cos(angle), s = Math.sin(angle), d = dot(axis, v);
        var k = cross(axis, v);
        return new double[]{v[0] * c + k[0] * s + axis[0] * d * (1 - c), v[1] * c + k[1] * s + axis[1] * d * (1 - c), v[2] * c + k[2] * s + axis[2] * d * (1 - c)};
    }

    private static void add(List<Triangle> out, Mesh.Polygon polygon, double x, double y, double z) {
        add(out, polygon, x, y, z, 0);
    }
    private static void add(List<Triangle> out, Mesh.Polygon polygon, double x, double y, double z, int colour) {
        double[] p = polygon.positions(), uv = polygon.uvs();
        int n = polygon.size();
        for (int i = 1; i + 1 < n; i++) {
            int[] c = {0, i, i + 1};
            double[][] pts = new double[3][], uvs = new double[3][];
            for (int k = 0; k < 3; k++) {
                pts[k] = new double[]{p[3 * c[k]] + x, p[3 * c[k] + 1] + y, p[3 * c[k] + 2] + z};
                uvs[k] = new double[]{uv[2 * c[k]], uv[2 * c[k] + 1]};
            }
            var normal = cross(sub(pts[1], pts[0]), sub(pts[2], pts[0]));
            double l = Math.sqrt(dot(normal, normal));
            if (l < 1e-12) continue;
            out.add(new Triangle(pts, uvs, new double[]{normal[0] / l, normal[1] / l, normal[2] / l}, polygon.material(), colour));
        }
    }

    /** The game's directional shade of a face normal (NeoForge {@code QuadLighter.calculateShade}, no constant ambient). */
    private static double shade(double[] n) {
        return Math.min(n[0] * n[0] * .6 + n[1] * n[1] * ((3 + n[1]) / 4) + n[2] * n[2] * .8, 1);
    }

    private static void render(List<Triangle> triangles, EquipmentShape shape, double[] depth, Map<Mesh.Material, Sprite> sprites, String file) throws Exception {
        double[] right = unit(Math.abs(depth[1]) > .99 ? new double[]{1, 0, 0} : cross(depth, new double[]{0, 1, 0}));
        double[] up = Math.abs(depth[1]) > .99 ? new double[]{0, 0, -1} : cross(right, depth);
        // The ground: two blocks round the box, with its block grid.
        int gx0 = -2, gz0 = -2, gx1 = shape.sizeX() + 2, gz1 = shape.sizeZ() + 2;
        double minX = 1e9, maxX = -1e9, minY = 1e9, maxY = -1e9;
        var corners = new ArrayList<double[]>();
        for (var t : triangles) corners.addAll(Arrays.asList(t.p));
        for (int x : new int[]{gx0, gx1})
            for (int z : new int[]{gz0, gz1}) corners.add(new double[]{x, 0, z});
        if(focus!=null){corners.clear();for(var t:triangles)for(var point:t.p)if(point[1]<=cropTop)corners.add(point);}
        for (var v : corners) {
            minX = Math.min(minX, dot(v, right));
            maxX = Math.max(maxX, dot(v, right));
            minY = Math.min(minY, dot(v, up));
            maxY = Math.max(maxY, dot(v, up));
        }
        double scale = zoom>0?zoom:Math.min(900,700/Math.max(maxX-minX,maxY-minY));
        int w = (int) Math.ceil((maxX - minX) * scale) + 20, h = (int) Math.ceil((maxY - minY) * scale) + 20;
        var image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        var zbuf = new double[w * h];
        Arrays.fill(zbuf, Double.MAX_VALUE);
        int sky = 0x9CC3F5;
        for (int i = 0; i < w * h; i++) image.setRGB(i % w, i / w, sky);
        double fx = minX, fy = minY;
        Rasterizer raster = (a, b, c, colour) -> {
            double[][] s = new double[3][];
            int k = 0;
            for (var v : new double[][]{a, b, c}) s[k++] = new double[]{(dot(v, right) - fx) * scale + 10, h - ((dot(v, up) - fy) * scale + 10), dot(v, depth)};
            int x0 = (int) Math.max(0, Math.floor(Math.min(s[0][0], Math.min(s[1][0], s[2][0])))), x1 = (int) Math.min(w - 1, Math.ceil(Math.max(s[0][0], Math.max(s[1][0], s[2][0]))));
            int y0 = (int) Math.max(0, Math.floor(Math.min(s[0][1], Math.min(s[1][1], s[2][1])))), y1 = (int) Math.min(h - 1, Math.ceil(Math.max(s[0][1], Math.max(s[1][1], s[2][1]))));
            double area = edge(s[0], s[1], s[2][0], s[2][1]);
            if (Math.abs(area) < 1e-12) return;
            for (int y = y0; y <= y1; y++)
                for (int x = x0; x <= x1; x++) {
                    double px = x + .5, py = y + .5;
                    double w0 = edge(s[1], s[2], px, py) / area, w1 = edge(s[2], s[0], px, py) / area, w2 = edge(s[0], s[1], px, py) / area;
                    if (w0 < 0 || w1 < 0 || w2 < 0) continue;
                    double z = w0 * s[0][2] + w1 * s[1][2] + w2 * s[2][2];
                    if (z >= zbuf[y * w + x]) continue;
                    int rgb = colour.at(w0, w1, w2);
                    if (rgb == -1) continue;
                    zbuf[y * w + x] = z;
                    int alpha=rgb>>>24;if(alpha==0)alpha=255;int back=image.getRGB(x,y);int red=((rgb>>16&255)*alpha+(back>>16&255)*(255-alpha))/255,green=((rgb>>8&255)*alpha+(back>>8&255)*(255-alpha))/255,blue=((rgb&255)*alpha+(back&255)*(255-alpha))/255;image.setRGB(x,y,red<<16|green<<8|blue);
                }
        };
        double lowest=triangles.stream().flatMap(t->Arrays.stream(t.p)).mapToDouble(p->p[1]).min().orElse(0);
        double ground=Math.min(0,lowest);
        // Match the support plane; the raster below is inset slightly to avoid coplanar faces.
        for (int x = gx0; x < gx1; x++)
            for (int z = gz0; z < gz1; z++) {
                double[] a = {x, ground-.001, z}, b = {x + 1, ground-.001, z}, c = {x + 1, ground-.001, z + 1}, d = {x, ground-.001, z + 1};
                int fx0 = x, fz0 = z;
                Colour grass = (w0, w1, w2) -> 0x6A9A3C;
                raster.fill(a, b, c, grass);
                raster.fill(a, c, d, grass);
                double t = .03;
                for (double[][] line : new double[][][]{{{fx0, ground-.0005, fz0}, {fx0 + 1, ground-.0005, fz0}, {fx0 + 1, ground-.0005, fz0 + t}, {fx0, ground-.0005, fz0 + t}},
                        {{fx0, ground-.0005, fz0}, {fx0 + t, ground-.0005, fz0}, {fx0 + t, ground-.0005, fz0 + 1}, {fx0, ground-.0005, fz0 + 1}}}) {
                    Colour dark = (w0, w1, w2) -> 0x557D30;
                    raster.fill(line[0], line[1], line[2], dark);
                    raster.fill(line[0], line[2], line[3], dark);
                }
            }
        for (var t : triangles) {
            if (dot(t.normal, depth) >= 0) continue; // back face: culled as the chunk renderer culls it
            var sprite = sprites.get(t.material);
            double light = shade(t.normal);
            int channel = t.material.channel();
            int tint = channel < 0 ? 0xFFFFFF : Paint.DEFAULT.colour(channel);
            double tr = Math.min(255, (tint >> 16 & 255) * 255.0 / Paint.SPRITE_VALUE) / 255, tg = Math.min(255, (tint >> 8 & 255) * 255.0 / Paint.SPRITE_VALUE) / 255,
                    tb = Math.min(255, (tint & 255) * 255.0 / Paint.SPRITE_VALUE) / 255;
            if (channel < 0) tr = tg = tb = 1;
            double ftr = tr, ftg = tg, ftb = tb;
            raster.fill(t.p[0], t.p[1], t.p[2], (w0, w1, w2) -> {
                int texel = t.colour != 0 ? t.colour : 0xFFFF00FF;
                if (t.colour == 0 && sprite != null) {
                    double u = w0 * t.uv[0][0] + w1 * t.uv[1][0] + w2 * t.uv[2][0], v = w0 * t.uv[0][1] + w1 * t.uv[1][1] + w2 * t.uv[2][1];
                    texel = sprite.sample(u, v);
                    if ((texel >>> 24) < 128) return -1; // cutout
                }
                int r = (int) Math.min(255, (texel >> 16 & 255) * ftr * light), g = (int) Math.min(255, (texel >> 8 & 255) * ftg * light),
                        b = (int) Math.min(255, (texel & 255) * ftb * light);
                return (texel&0xff000000)|r << 16 | g << 8 | b;
            });
        }
        ImageIO.write(image, "png", new File(file));
    }

    private interface Colour {
        /** The colour at barycentric weights, or -1 for none (a cutout texel). */
        int at(double w0, double w1, double w2);
    }

    private interface Rasterizer {
        void fill(double[] a, double[] b, double[] c, Colour colour);
    }

    private static double edge(double[] a, double[] b, double x, double y) {
        return (b[0] - a[0]) * (y - a[1]) - (b[1] - a[1]) * (x - a[0]);
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static double[] sub(double[] a, double[] b) {
        return new double[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static double[] unit(double[] a) {
        double l = Math.sqrt(dot(a, a));
        return new double[]{a[0] / l, a[1] / l, a[2] / l};
    }
}

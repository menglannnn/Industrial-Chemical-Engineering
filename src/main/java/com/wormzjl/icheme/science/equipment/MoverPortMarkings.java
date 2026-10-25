package com.wormzjl.icheme.science.equipment;

import com.wormzjl.icheme.science.math.DeterministicMath;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

/** Static flow arrows on constructed mover nozzles and valve castings, independent of process state and collision. */
public final class MoverPortMarkings {
    private MoverPortMarkings() {}

    /** One arrow band on each process nozzle, clipped into the equipment cells that own its neck. */
    public static List<Mesh.Polygon> surface(ShapeRasterizer.CellKey key) {
        if(!key.shape().kind().mover())return List.of();
        var cell=new ShapeRasterizer.Cell(key.x(),key.y(),key.z());
        var nozzles=key.shape().nozzles();var out=new ArrayList<Mesh.Polygon>();int reaching=0;
        for(int k=0;k<nozzles.size();k++) {
            if(!ShapeRasterizer.nozzleCells(key.shape(),k).contains(cell))continue;
            int code=(int)(key.nozzles()>>>(7*reaching++))&127;
            var nozzle=nozzles.get(k);
            if(nozzle.role()!=EquipmentShape.Role.SUCTION&&nozzle.role()!=EquipmentShape.Role.DISCHARGE)continue;
            double od=code==0?nozzle.defaultOd():PipeSizes.outsideOf(code);
            var mesh=new Mesh();mesh.transform(key.shape().transform());mark(mesh,nozzle,od);
            out.addAll(mesh.cell(cell.x(),cell.y(),cell.z()));
        }
        return List.copyOf(out);
    }

    private static void mark(Mesh mesh,EquipmentShape.Nozzle nozzle,double od) {
        int sides=PipeSizes.sides(od),flow=nozzle.role()==EquipmentShape.Role.SUCTION?-1:1;
        // Stay on the equipment neck, clear of the flange plate and its bolts.
        double end=nozzle.length()-PipeSizes.flangeThickness(od)-.055;
        double length=Math.min(.65,Math.min(end-.02,Math.max(.28,1.6*od)));
        if(length<=.02)return;
        double width=.82*od*DeterministicMath.sin(Math.PI/sides);
        double radius=od/2*DeterministicMath.cos(Math.PI/sides);
        var frame=Mesh.Frame.of(nozzle.base(),nozzle.axis(),nozzle.sign());var tangent=frame.side();
        for(int side=0;side<4;side++) {
            double angle=2*Math.PI*(side*sides/4+.5)/sides;
            double c=DeterministicMath.cos(angle),s=DeterministicMath.sin(angle);var normal=new double[3];
            for(int i=0;i<3;i++)normal[i]=c*frame.ref()[i]+s*tangent[i];
            badge(new Label(mesh,new Mesh.Frame(frame.origin(),frame.axis(),normal),radius,end-length/2,flow),length,width);
        }
    }

    private record Plane(long nx,long ny,long nz,long radius) {}
    private static final class Patch {
        final double[] normal;final double radius;
        double low=Double.POSITIVE_INFINITY,high=Double.NEGATIVE_INFINITY,left=low,right=high;
        Patch(double[] normal,double radius){this.normal=normal;this.radius=radius;}
    }

    private static void badge(Label label,double length,double width) {
        label.rect(-length/2,-width/2,length/2,width/2,.001,Mesh.Material.OPENING);
        label.rect(-.48*length,-.45*width,.48*length,.45*width,.0015,Mesh.Material.GUARD);
        // Two convex pieces keep the arrowhead sharp through the mesh's normal cell clipping.
        label.rect(-.4*length,-.13*width,.16*length,.13*width,.002,Mesh.Material.OPENING);
        label.polygon(List.of(new double[]{.12*length,-.35*width},new double[]{.42*length,0},new double[]{.12*length,.35*width}),.002,Mesh.Material.OPENING);
    }
    /** Paint on actual axial casting facets, within their available station/width bounds. */
    public static List<Mesh.Polygon> fitting(List<Mesh.Polygon> surface,int facing,double from,double to) {
        return fitting(surface,facing,from,to,Math.PI/8);
    }
    /** A bonnet can leave diagonal facets clearer than the cardinal panels of a plain casting. */
    static List<Mesh.Polygon> fitting(List<Mesh.Polygon> surface,int facing,double from,double to,double angleOffset) {return fitting(surface,facing,from,to,angleOffset,false);}
    /** Two lateral arrow panels remain clear of an upright motor/gearbox, including overhanging castings. */
    static List<Mesh.Polygon> fittingOverhanging(List<Mesh.Polygon> surface,int facing,double from,double to){return fitting(surface,facing,from,to,0,true);}
    private static List<Mesh.Polygon> fitting(List<Mesh.Polygon> surface,int facing,double from,double to,double angleOffset,boolean overhang) {
        var mesh=new Mesh();var axis=LineGeometry.step(facing);var patches=new LinkedHashMap<Plane,Patch>();
        for(var polygon:surface) {
            if(polygon.material()!=Mesh.Material.FLANGE||polygon.size()<3)continue;
            double[] p=polygon.positions(),a={p[3]-p[0],p[4]-p[1],p[5]-p[2]},b={p[6]-p[0],p[7]-p[1],p[8]-p[2]};
            var normal=Mesh.cross(a,b);double norm=Math.sqrt(Mesh.dot(normal,normal));if(norm<1e-9)continue;
            for(int i=0;i<3;i++)normal[i]/=norm;
            if(Math.abs(Mesh.dot(normal,axis))>1e-6)continue;
            var side=Mesh.cross(axis,normal);
            double low=Double.POSITIVE_INFINITY,high=Double.NEGATIVE_INFINITY,left=low,right=high,radius=0;
            for(int i=0;i<p.length;i+=3) {
                double[] delta={p[i]-.5,p[i+1]-.5,p[i+2]-.5};
                double along=Mesh.dot(delta,axis),across=Mesh.dot(delta,side);
                low=Math.min(low,along);high=Math.max(high,along);left=Math.min(left,across);right=Math.max(right,across);radius=Mesh.dot(delta,normal);
            }
            low=Math.max(from,low);high=Math.min(to,high);if(high<=low||radius<=0)continue;
            var plane=new Plane(Math.round(normal[0]*1e6),Math.round(normal[1]*1e6),Math.round(normal[2]*1e6),Math.round(radius*1e6));
            var patch=patches.computeIfAbsent(plane,k->new Patch(normal,Mesh.dot(new double[]{p[0]-.5,p[1]-.5,p[2]-.5},normal)));
            patch.low=Math.min(patch.low,low);patch.high=Math.max(patch.high,high);patch.left=Math.min(patch.left,left);patch.right=Math.max(patch.right,right);
        }
        int bodyFace=facing|1;var reference=LineGeometry.ref(bodyFace);var tangent=Mesh.cross(LineGeometry.step(bodyFace),reference);var selected=new LinkedHashSet<Patch>();
        for(int q=0;q<(overhang?2:4);q++) {
            double angle=overhang?(reference[1]==0?0:Math.PI/2)+q*Math.PI:angleOffset+q*Math.PI/2;double c=DeterministicMath.cos(angle),s=DeterministicMath.sin(angle),best=-2;Patch choice=null;
            for(var patch:patches.values()) {double score=c*Mesh.dot(patch.normal,reference)+s*Mesh.dot(patch.normal,tangent);if(score>best){best=score;choice=patch;}}
            if(choice!=null)selected.add(choice);
        }
        for(var patch:selected) {
            var side=Mesh.cross(axis,patch.normal);double[] origin={.5,.5,.5};
            for(int i=0;i<3;i++)origin[i]+=.5*(patch.left+patch.right)*side[i];
            badge(new Label(mesh,new Mesh.Frame(origin,axis,patch.normal),patch.radius,(patch.low+patch.high)/2,1),.92*(patch.high-patch.low),.80*(patch.right-patch.left));
        }
        return overhang?LineGeometry.overhanging(mesh):List.copyOf(mesh.cell(0,0,0));
    }

    /** Local axes keep arrows aligned with flow for either nozzle role and every equipment heading. */
    private record Label(Mesh mesh,Mesh.Frame frame,double radius,double station,int flow) {
        void rect(double x0,double y0,double x1,double y1,double lift,Mesh.Material material) {
            polygon(List.of(new double[]{x0,y0},new double[]{x1,y0},new double[]{x1,y1},new double[]{x0,y1}),lift,material);
        }
        void polygon(List<double[]> points,double lift,Mesh.Material material) {
            double[] normal=frame.ref(),side=frame.side(),axis=frame.axis();
            var centre=frame.along(station);var polygon=new ArrayList<double[]>(points.size());
            for(var point:points) {
                var p=new double[3];
                for(int a=0;a<3;a++)p[a]=centre[a]+(radius+lift)*normal[a]+flow*point[0]*axis[a]-flow*point[1]*side[a];
                polygon.add(p);
            }
            // Solid paint samples avoid borrowing the guard's perforation pattern for a small, legible stencil.
            var u=new Mesh.Uv(new double[3],new double[3],material==Mesh.Material.GUARD?1.5/16:8.5/16,1);
            var v=new Mesh.Uv(new double[3],new double[3],.5/16,1);
            var inside=centre.clone();for(int a=0;a<3;a++)inside[a]+=(radius+lift-1)*normal[a];
            mesh.add(polygon,inside,Mesh.FLAT,material,u,v);
        }
    }
}

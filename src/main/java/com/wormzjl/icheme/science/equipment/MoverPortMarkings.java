package com.wormzjl.icheme.science.equipment;

import com.wormzjl.icheme.science.math.DeterministicMath;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

/** Static painted flow arrows on constructed movers' nozzle necks, independent of process state and collision. */
public final class MoverPortMarkings {
    private MoverPortMarkings() {}

    /**
     * Cell-local decals using the same seven-bit, nozzle-order diameter codes as the shell's surface. A suction arrow
     * points into the machine, a discharge arrow out of it; decorative coolant connections receive no process marking.
     */
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

    /** Arrows on the actual exposed pipe facets adjoining mover ports: two role bits per Minecraft face. */
    public static List<Mesh.Polygon> pipe(List<Mesh.Polygon> surface,int roles) {
        if(roles==0)return List.of();
        var mesh=new Mesh();double[] centre={.5,.5,.5};
        for(int face=0;face<6;face++) {
            int role=roles>>>(2*face)&3;if(role!=1&&role!=2)continue;
            double[] axis=LineGeometry.step(face);
            var patches=new LinkedHashMap<Plane,Patch>();
            for(var polygon:surface) {
                if(polygon.material()!=Mesh.Material.PIPE||polygon.size()<3)continue;
                double[] p=polygon.positions(),a={p[3]-p[0],p[4]-p[1],p[5]-p[2]},b={p[6]-p[0],p[7]-p[1],p[8]-p[2]};
                var normal=Mesh.cross(a,b);double norm=Math.sqrt(Mesh.dot(normal,normal));if(norm<1e-9)continue;
                for(int i=0;i<3;i++)normal[i]/=norm;
                if(Math.abs(Mesh.dot(normal,axis))>1e-6)continue; // Flat axial necks only, never reducers or window faces.
                var side=Mesh.cross(axis,normal);
                double low=Double.POSITIVE_INFINITY,high=Double.NEGATIVE_INFINITY,left=low,right=high,radius=0;
                for(int i=0;i<p.length;i+=3) {
                    double[] delta={p[i]-.5,p[i+1]-.5,p[i+2]-.5};
                    double along=Mesh.dot(delta,axis),across=Mesh.dot(delta,side);
                    low=Math.min(low,along);high=Math.max(high,along);left=Math.min(left,across);right=Math.max(right,across);
                    radius=Mesh.dot(delta,normal);
                }
                low=Math.max(.12,low);high=Math.min(.42,high);
                if(high<=low||radius<=0)continue;
                var plane=new Plane(Math.round(normal[0]*1e6),Math.round(normal[1]*1e6),Math.round(normal[2]*1e6),Math.round(radius*1e6));
                var patch=patches.get(plane);
                if(patch==null){patch=new Patch(normal,radius);patches.put(plane,patch);}
                patch.low=Math.min(patch.low,low);patch.high=Math.max(patch.high,high);
                patch.left=Math.min(patch.left,left);patch.right=Math.max(patch.right,right);
            }
            // The hub and neck, and UV tile boundaries, can split one plane. Paint one arrow over their union.
            var selected=new LinkedHashSet<Patch>();var reference=LineGeometry.ref(face);var tangent=Mesh.cross(axis,reference);
            for(int q=0;q<4;q++) {
                double angle=Math.PI/8+q*Math.PI/2,c=DeterministicMath.cos(angle),s=DeterministicMath.sin(angle),best=-2;
                Patch choice=null;
                for(var patch:patches.values()) {
                    double score=c*Mesh.dot(patch.normal,reference)+s*Mesh.dot(patch.normal,tangent);
                    if(score>best){best=score;choice=patch;}
                }
                if(choice!=null)selected.add(choice);
            }
            for(var patch:selected) {
                double low=patch.low+.003,high=patch.high-.003,length=Math.min(.28,high-low),width=.82*(patch.right-patch.left);
                if(length<.025||width<.005)continue;
                var side=Mesh.cross(axis,patch.normal);var origin=centre.clone();
                for(int i=0;i<3;i++)origin[i]+=.5*(patch.left+patch.right)*side[i];
                badge(new Label(mesh,new Mesh.Frame(origin,axis,patch.normal),0,patch.radius,(low+high)/2,role==1?1:-1),length,width);
            }
        }
        return List.copyOf(mesh.cell(0,0,0));
    }

    private record Plane(long nx,long ny,long nz,long radius) {}
    private static final class Patch {
        final double[] normal;final double radius;
        double low=Double.POSITIVE_INFINITY,high=Double.NEGATIVE_INFINITY,left=low,right=high;
        Patch(double[] normal,double radius){this.normal=normal;this.radius=radius;}
    }

    private static void mark(Mesh mesh,EquipmentShape.Nozzle nozzle,double od) {
        int sides=PipeSizes.sides(od),flow=nozzle.role()==EquipmentShape.Role.SUCTION?-1:1;
        double end=nozzle.length()-PipeSizes.flangeThickness(od)-.055;
        double length=Math.min(.65,Math.min(end-.02,Math.max(.28,1.6*od)));
        if(length<=.02)return;
        double width=.82*od*DeterministicMath.sin(Math.PI/sides);
        double radius=od/2*DeterministicMath.cos(Math.PI/sides);
        var frame=Mesh.Frame.of(nozzle.base(),nozzle.axis(),nozzle.sign());
        for(int side=0;side<4;side++) {
            double angle=2*Math.PI*(side*sides/4+.5)/sides;
            var label=new Label(mesh,frame,angle,radius,end-length/2,flow);
            badge(label,length,width);
        }
    }

    private static void badge(Label label,double length,double width) {
        label.rect(-length/2,-width/2,length/2,width/2,.001,Mesh.Material.OPENING);
        label.rect(-.48*length,-.45*width,.48*length,.45*width,.0015,Mesh.Material.GUARD);
        // Two convex pieces keep the arrowhead sharp through the mesh's normal cell clipping.
        label.rect(-.4*length,-.13*width,.16*length,.13*width,.002,Mesh.Material.OPENING);
        label.polygon(List.of(new double[]{.12*length,-.35*width},new double[]{.42*length,0},new double[]{.12*length,.35*width}),.002,Mesh.Material.OPENING);
    }

    /** Local axes keep arrows aligned with flow for either nozzle role and every equipment heading. */
    private record Label(Mesh mesh,Mesh.Frame frame,double angle,double radius,double station,int flow) {
        void rect(double x0,double y0,double x1,double y1,double lift,Mesh.Material material) {
            polygon(List.of(new double[]{x0,y0},new double[]{x1,y0},new double[]{x1,y1},new double[]{x0,y1}),lift,material);
        }
        void polygon(List<double[]> points,double lift,Mesh.Material material) {
            double c=DeterministicMath.cos(angle),s=DeterministicMath.sin(angle);
            double[] ref=frame.ref(),side=frame.side(),axis=frame.axis();
            var normal=new double[3];for(int a=0;a<3;a++)normal[a]=c*ref[a]+s*side[a];
            var centre=frame.along(station);var polygon=new ArrayList<double[]>(points.size());
            for(var point:points) {
                var p=new double[3];
                for(int a=0;a<3;a++)p[a]=centre[a]+(radius+lift)*normal[a]+flow*point[0]*axis[a]-flow*point[1]*(-s*ref[a]+c*side[a]);
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

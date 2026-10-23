package com.wormzjl.icheme.science.equipment;

import java.util.List;
import com.wormzjl.icheme.science.math.DeterministicMath;
import static com.wormzjl.icheme.science.equipment.Mesh.*;

/** Diameter-driven round-window accessory. The viewing bore is genuinely open through both sides of the housing. */
public final class SightGlassGeometry {
    private static final double[] CENTRE={.5,.5,.5},UP={0,1,0};
    private static final int SIDES=24;
    private SightGlassGeometry(){}

    public record Geometry(List<Polygon> metal,List<Polygon> glass,List<Polygon> liquid) {
        public Geometry{metal=List.copyOf(metal);glass=List.copyOf(glass);liquid=List.copyOf(liquid);}
    }

    /** Housing and all connected arms at this pipe's existing geometry; accessory items show the housing alone. */
    public static Geometry build(LineGeometry.Key key,boolean accessory) {
        if(!accessory&&Integer.bitCount(key.connections())>2)return new Geometry(LineGeometry.polygons(key),List.of(),List.of());
        double od=PipeSizes.outsideOf(key.outside());
        // The body and viewing aperture both scale across the entire pipe range. The minimum is a readable fitting,
        // not a second hydraulic bore. The largest fits the cell, as the existing line flanges do.
        double radius=.065+.375*od,window=.66*radius,depth=.035+.24*radius,rim=.12*radius;
        int mask=key.connections();
        if(Integer.bitCount(mask)<=1){int face=mask==0?key.facing():Integer.numberOfTrailingZeros(mask);mask=(1<<face)|(1<<(face^1));}
        double[] view=viewAxis(mask);
        var frame=new Frame(CENTRE,view,UP);var metal=new Mesh();var glass=new Mesh();var liquid=new Mesh();
        metal.ring(frame,window,radius*.92,-depth,depth,SIDES,3,Material.PIPE);
        for(int sign:new int[]{-1,1}) {
            var side=new Frame(CENTRE,new double[]{sign*view[0],0,sign*view[2]},UP);
            metal.ring(side,window,radius,depth,depth+rim,SIDES,3,Material.FLANGE);
            // Window-cover fasteners scale with the cover, independently of the pipeline flange's standard bolts.
            for(int bolt=0;bolt<8;bolt++)metal.cylinder(new Frame(side.at(2*Math.PI*(bolt+.5)/8,(window+radius)/2,depth+rim),side.axis(),UP),
                    .13*radius/Math.sqrt(3),0,.1*radius,6,Material.BOLT);
            glass.annulus(side,0,window,depth+rim*.65,SIDES,1,Material.GLASS);
            liquid.annulus(side,0,window*.995,depth+rim*.3,SIDES,1,Material.GLASS);
        }
        var arms=new Mesh();
        if(!accessory)for(int face=0;face<6;face++)if((mask>>face&1)!=0) {
            var end=LineGeometry.end(key,face,od);
            end=new LineGeometry.End(end.outside(),true,end.reducer(),end.reducer()+PipeSizes.flangeThickness(end.outside())+PipeSizes.hub(end.outside()),end.joined());
            double dot=LineGeometry.STEPS[face][0]*view[0]+LineGeometry.STEPS[face][2]*view[2];
            double radial=.92*radius/Math.sqrt(Math.max(.25,1-dot*dot));
            double axial=Math.abs(dot)<1e-9?Double.POSITIVE_INFINITY:depth/Math.abs(dot);
            double from=.9*Math.min(.49,Math.min(radial,axial));
            LineGeometry.arm(arms,face,from,od,end,LineGeometry.ref(face),PipeSizes.sides(od),false);
        }
        // Nozzle walls can cross a diagonal bend's view. Cut the same polygonal viewing bore through those walls
        // as through the housing, retaining their existing UV frames and normals rather than hiding a window behind metal.
        metal.include(clearWindow(arms,frame,window));
        return new Geometry(metal.cell(0,0,0),glass.cell(0,0,0),liquid.cell(0,0,0));
    }

    private static Mesh clearWindow(Mesh source,Frame frame,double radius) {
        var out=new Mesh();var side=frame.side();double[][] normals=new double[SIDES][3];double[] offsets=new double[SIDES];
        for(int i=0;i<SIDES;i++) {
            double angle=2*Math.PI*(i+.5)/SIDES,c=DeterministicMath.cos(angle),s=DeterministicMath.sin(angle);
            for(int axis=0;axis<3;axis++)normals[i][axis]=c*frame.ref()[axis]+s*side[axis];
            offsets[i]=-dot(normals[i],CENTRE)-radius*DeterministicMath.cos(Math.PI/SIDES);
        }
        for(var facet:source.facets()) {
            var overlap=facet.positions();
            for(int i=0;i<SIDES&&overlap.size()>=3;i++)overlap=clip(overlap,neg(normals[i]),-offsets[i]);
            if(overlap.size()<3||area(overlap)<1e-12){append(out,facet.positions(),facet);continue;}
            var remaining=facet.positions();
            for(int i=0;i<SIDES&&remaining.size()>=3;i++) {
                var outside=clip(remaining,normals[i],offsets[i]);
                if(outside.size()>=3&&area(outside)>1e-12)append(out,outside,facet);
                remaining=clip(remaining,neg(normals[i]),-offsets[i]);
            }
        }
        return out;
    }
    private static void append(Mesh mesh,List<double[]> polygon,Facet source) {
        double[] inside=new double[3];for(var p:polygon)for(int axis=0;axis<3;axis++)inside[axis]+=p[axis]/polygon.size();
        for(int axis=0;axis<3;axis++)inside[axis]-=source.normal()[axis];
        mesh.add(polygon,inside,source.shading(),source.material(),source.u(),source.v());
    }

    /** Always horizontal: a free side axis, north/south for vertical lines, or the diagonal between bend axes. */
    private static double[] viewAxis(int mask) {
        boolean x=(mask&48)!=0,z=(mask&12)!=0;
        if(x&&z){double d=1/Math.sqrt(2);return new double[]{d,0,d};}
        return z?new double[]{1,0,0}:new double[]{0,0,1};
    }
}

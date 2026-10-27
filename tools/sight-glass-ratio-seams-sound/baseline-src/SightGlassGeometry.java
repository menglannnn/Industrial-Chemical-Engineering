package com.wormzjl.icheme.science.equipment;

import java.util.List;
import java.util.Objects;
import com.wormzjl.icheme.science.math.DeterministicMath;
import static com.wormzjl.icheme.science.equipment.Mesh.*;

/** Diameter-driven round-window accessory. The viewing bore is genuinely open through both sides of the housing. */
public final class SightGlassGeometry {
    private static final double[] CENTRE={.5,.5,.5},UP={0,1,0};
    private static final int SIDES=32;
    private SightGlassGeometry(){}

    /** World-coordinate axis of the paired window normals. */
    public enum Axis {
        X,Y,Z;
        private double coordinate(double x,double y,double z){return switch(this){case X->x;case Y->y;case Z->z;};}
        private double[] vector(){return switch(this){case X->new double[]{1,0,0};case Y->new double[]{0,1,0};case Z->new double[]{0,0,1};};}
    }

    /** A visual key, independent of the pipe's hydraulic facing. Window axes always remain perpendicular to the run. */
    public record Key(LineGeometry.Key pipe,Axis window) {
        public Key {
            Objects.requireNonNull(pipe);Objects.requireNonNull(window);
            var run=run(pipe.connections(),pipe.facing());if(window==run)window=defaultWindow(run);
        }
        public Key(LineGeometry.Key pipe){this(pipe,defaultWindow(run(pipe.connections(),pipe.facing())));}
    }

    /** The permitted window axis closest to the installer, using the eye offset from the block centre. */
    public static Axis towards(int connections,int facing,double x,double y,double z) {
        var run=run(connections,facing);var chosen=defaultWindow(run);
        for(var axis:Axis.values())if(axis!=run&&Math.abs(axis.coordinate(x,y,z))>Math.abs(chosen.coordinate(x,y,z)))chosen=axis;
        return chosen;
    }
    private static Axis run(int connections,int facing) {
        int face=connections==0?facing:Integer.numberOfTrailingZeros(connections);
        return switch(LineGeometry.axis(face)){case 0->Axis.X;case 1->Axis.Y;default->Axis.Z;};
    }
    private static Axis defaultWindow(Axis run){return run==Axis.Z?Axis.X:Axis.Z;}

    public record Geometry(List<Polygon> metal,List<Polygon> glass,List<Polygon> liquid) {
        public Geometry{metal=List.copyOf(metal);glass=List.copyOf(glass);liquid=List.copyOf(liquid);}
    }
    /** Shared dynamic-overlay aperture, with its plane between liquid and clear glass. */
    public record Window(double radius,double plane,Axis axis){
        public Window{Objects.requireNonNull(axis);if(!Double.isFinite(radius)||!Double.isFinite(plane)||radius<=0||plane<=0)throw new IllegalArgumentException("Invalid window aperture");}
    }
    private record Size(double radius,double depth){}
    private static Size size(Key look){double od=PipeSizes.outsideOf(look.pipe().outside());return new Size(.09+.37*od,.04+.34*od);}
    public static Window window(Key look){var size=size(look);return new Window(.60*.995*size.radius(),size.depth()+.085*size.radius(),look.window());}

    /** Unconnected and dead-end pipes draw straight; two contacts must be opposite. Corners and junctions are excluded. */
    public static boolean supports(int connections){return Integer.bitCount(connections)<=1||connections==3||connections==12||connections==48;}

    /** Housing and straight arms; static windows and transient liquid share the same saved visual orientation. */
    public static Geometry build(Key look,boolean accessory) {
        var key=look.pipe();
        if(!accessory&&!supports(key.connections()))return new Geometry(LineGeometry.polygons(key),List.of(),List.of());
        double od=PipeSizes.outsideOf(key.outside());
        // The body and viewing aperture both scale across the entire pipe range. The minimum is a readable fitting,
        // not a second hydraulic bore. The largest fits the cell, as the existing line flanges do.
        var size=size(look);double radius=size.radius(),window=.60*radius,depth=size.depth();
        int mask=key.connections();
        if(Integer.bitCount(mask)<=1){int face=mask==0?key.facing():Integer.numberOfTrailingZeros(mask);mask=(1<<face)|(1<<(face^1));}
        double[] view=look.window().vector(),reference=look.window()==Axis.Y?new double[]{0,0,1}:UP;
        var frame=new Frame(CENTRE,view,reference);var metal=new Mesh();var glass=new Mesh();var liquid=new Mesh();
        windowPlate(metal,frame,window,.93*radius,.93*radius,-depth,depth,3,Material.PIPE);
        for(int sign:new int[]{-1,1}) {
            var side=new Frame(CENTRE,new double[]{sign*view[0],sign*view[1],sign*view[2]},reference);
            // A cast chamber, dark cover joint and bevelled four-bolt cover distinguish the fitting from a bare washer.
            windowPlate(metal,side,window,.96*radius,.96*radius,depth,depth+.025*radius,3,Material.OPENING);
            windowPlate(metal,side,.66*radius,radius,radius,depth+.025*radius,depth+.10*radius,1,Material.FLANGE);
            windowPlate(metal,side,.66*radius,radius,.96*radius,depth+.10*radius,depth+.14*radius,2,Material.FLANGE);
            metal.ring(side,window,.72*radius,depth+.10*radius,depth+.14*radius,SIDES,1,Material.FLANGE);
            metal.frustum(side,.72*radius,.66*radius,depth+.14*radius,depth+.20*radius,SIDES,Material.FLANGE);
            metal.bore(side,window,depth+.14*radius,depth+.20*radius,SIDES,Material.FLANGE);
            metal.annulus(side,window,.66*radius,depth+.20*radius,SIDES,1,Material.FLANGE);
            // Recessed gasket and glass remain behind the circular retaining lip, leaving a clear opening through the body.
            metal.ring(side,window*.98,.64*radius,depth+.07*radius,depth+.095*radius,SIDES,3,Material.OPENING);
            for(int bolt=0;bolt<4;bolt++) {
                var fastener=new Frame(side.at(Math.PI/4+Math.PI*bolt/2,Math.sqrt(2)*.72*radius,depth+.14*radius),side.axis(),reference);
                metal.cylinder(fastener,.13*radius,0,.012*radius,12,Material.FLANGE);
                metal.cylinder(fastener,.18*radius/Math.sqrt(3),.012*radius,.077*radius,6,Material.BOLT);
                metal.cylinder(fastener,.045*radius,.077*radius,.092*radius,8,Material.BOLT);
            }
            glass.annulus(side,0,window,depth+.09*radius,SIDES,1,Material.GLASS);
            liquid.annulus(side,0,window*.995,depth+.08*radius,SIDES,1,Material.GLASS);
        }
        if(!accessory)for(int face=0;face<6;face++)if((mask>>face&1)!=0) {
            var end=LineGeometry.end(key,face,od);
            end=new LineGeometry.End(end.outside(),true,end.reducer(),end.reducer()+PipeSizes.flangeThickness(end.outside())+PipeSizes.hub(end.outside()),end.joined());
            LineGeometry.arm(metal,face,.90*radius,od,end,LineGeometry.ref(face),Math.max(16,PipeSizes.sides(od)),false);
        }
        return new Geometry(metal.cell(0,0,0),glass.cell(0,0,0),liquid.cell(0,0,0));
    }

    /** A chamfered square casting/cover with a circular bore. Different end widths make the cover's edge bevel. */
    private static void windowPlate(Mesh mesh,Frame frame,double bore,double half0,double half1,double z0,double z1,int caps,Material material) {
        mesh.bore(frame,bore,z0,z1,SIDES,material);
        for(int i=0;i<SIDES;i++) {
            double a=2*Math.PI*i/SIDES,b=2*Math.PI*(i+1)/SIDES;
            var p0=frame.at(a,outline(a,half0),z0);var p1=frame.at(b,outline(b,half0),z0);
            var p2=frame.at(b,outline(b,half1),z1);var p3=frame.at(a,outline(a,half1),z1);
            mesh.add(List.of(p0,p1,p2,p3),frame.along((z0+z1)/2),FLAT,material,null,null);
            if((caps&1)!=0)mesh.flat(List.of(frame.at(a,bore,z0),frame.at(b,bore,z0),p1,p0),neg(frame.axis()),material);
            if((caps&2)!=0)mesh.flat(List.of(frame.at(a,bore,z1),frame.at(b,bore,z1),p2,p3),frame.axis(),material);
        }
    }
    private static double outline(double angle,double half) {
        double c=Math.abs(DeterministicMath.cos(angle)),s=Math.abs(DeterministicMath.sin(angle));
        return half*Math.min(1/Math.max(c,s),1.68/(c+s));
    }
}

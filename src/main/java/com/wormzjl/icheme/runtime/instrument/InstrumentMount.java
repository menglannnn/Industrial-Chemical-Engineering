package com.wormzjl.icheme.runtime.instrument;

import com.wormzjl.icheme.world.item.GaugeSettings;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.Set;

/** One source of truth for the dial transform, interaction bounds and ghost placement. */
public record InstrumentMount(String id,Vec3 centre,Direction face,Set<GaugeSettings.Kind> kinds,boolean top,Vec3 tap,boolean bottomEntry,double probeDepth) implements AccessorySelection {
    public InstrumentMount(String id,Vec3 centre,Direction face,Set<GaugeSettings.Kind> kinds,boolean top){this(id,centre,face,kinds,top,centre.add(Vec3.atLowerCornerOf(face.getNormal()).scale(-.17)),false,.12);}
    public InstrumentMount{kinds=Set.copyOf(kinds);if(!Double.isFinite(probeDepth)||probeDepth<=0)throw new IllegalArgumentException("Invalid probe depth");if(face.getAxis()==Direction.Axis.Y)throw new IllegalArgumentException("Instrument face must stay upright");}
    public Vec3 normal(){return Vec3.atLowerCornerOf(face.getNormal());}
    public Vec3 right(){var n=normal();return new Vec3(n.z,0,-n.x);}
    public Vec3 point(double x,double y,double depth){return centre.add(right().scale(x)).add(0,y,0).add(normal().scale(depth));}
    public AABB bounds(){return new AABB(point(-.175,-.175,-.07),point(.175,.175,.07)).minmax(new AABB(tap.add(-.03,-.03,-.03),tap.add(.03,.03,.03))).inflate(.008);}
    public double[] localTap(){var delta=tap.subtract(centre);return new double[]{delta.dot(right()),delta.y,delta.dot(normal())};}
}

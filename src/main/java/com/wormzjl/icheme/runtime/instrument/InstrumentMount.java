package com.wormzjl.icheme.runtime.instrument;

import com.wormzjl.icheme.world.item.GaugeSettings;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.Set;

/** One source of truth for the dial transform, interaction bounds and ghost placement. */
public record InstrumentMount(String id,Vec3 centre,Direction face,Set<GaugeSettings.Kind> kinds,boolean top) implements AccessorySelection {
    public InstrumentMount{kinds=Set.copyOf(kinds);if(face.getAxis()==Direction.Axis.Y)throw new IllegalArgumentException("Instrument face must stay upright");}
    public Vec3 normal(){return Vec3.atLowerCornerOf(face.getNormal());}
    public Vec3 right(){var n=normal();return new Vec3(n.z,0,-n.x);}
    public Vec3 point(double x,double y,double depth){return centre.add(right().scale(x)).add(0,y,0).add(normal().scale(depth));}
    public AABB bounds(){var p=point(-.16,-.16,-.18);var q=point(.16,.16,.065);return new AABB(p,q).inflate(.015);}
}

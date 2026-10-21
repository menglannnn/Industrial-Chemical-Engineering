package com.wormzjl.icheme.science.fluid.network;

import com.wormzjl.icheme.science.fluid.state.SolidInventory;
import java.util.Objects;

/** Owned cake and carried energy, separate from all fluid reservoir inventories. */
public record InlineFilter(double capacity, double cleanResistance, SolidInventory captured, double energyJoule,boolean stoppedAtCapacity) {
    public InlineFilter(double capacity,double cleanResistance,SolidInventory captured,double energyJoule){this(capacity,cleanResistance,captured,energyJoule,false);}
    public InlineFilter {
        Objects.requireNonNull(captured);
        if(!Double.isFinite(capacity)||capacity<=0||!Double.isFinite(cleanResistance)||cleanResistance<=0
                ||!Double.isFinite(energyJoule)||captured.empty()&&energyJoule!=0)throw new IllegalArgumentException("Invalid filter state");
    }
    /** A clean filter with the default capacity and resistance ({@link com.wormzjl.icheme.science.fluid.transport.SlurryTransport.Settings#defaults}). */
    public static InlineFilter empty(){var d=com.wormzjl.icheme.science.fluid.transport.SlurryTransport.Settings.defaults();return new InlineFilter(d.filterCapacity(),d.filterResistance(),SolidInventory.EMPTY,0);}
    public double loading(){return captured.volume()/capacity;}
    public boolean clogged(){return stoppedAtCapacity||captured.volume()>=capacity;}
    public InlineFilter stopped(){return new InlineFilter(capacity,cleanResistance,captured,energyJoule,true);}
    public InlineFilter add(SolidInventory material,double energy){if(material.empty()&&energy==0)return this;return new InlineFilter(capacity,cleanResistance,captured.plus(material),energyJoule+energy,stoppedAtCapacity);}
    public InlineFilter cleared(){return new InlineFilter(capacity,cleanResistance,SolidInventory.EMPTY,0);}
}
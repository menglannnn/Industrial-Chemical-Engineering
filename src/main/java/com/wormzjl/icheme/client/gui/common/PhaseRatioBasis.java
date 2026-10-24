package com.wormzjl.icheme.client.gui.common;

import com.wormzjl.icheme.science.fluid.state.SolidInventory;
import java.util.List;

/** Phase fractions from cached amounts only; custom solids have no molecular identity in the material model. */
public enum PhaseRatioBasis {
    VOLUME,MOLAR,MASS;
    public PhaseRatioBasis next(){return switch(this){case VOLUME->MOLAR;case MOLAR->MASS;case MASS->VOLUME;};}
    public String key(){return switch(this){case VOLUME->"volume";case MOLAR->"mole";case MASS->"mass";};}
    public double[] amounts(double[] volumes,double[][] moles,SolidInventory solids,List<String> components,List<Double> weights) {
        if(volumes.length!=3||moles.length!=3||weights.size()!=components.size())throw new IllegalArgumentException("Invalid phase ratio axis");
        for(double weight:weights)if(!Double.isFinite(weight)||weight<=0)throw new IllegalArgumentException("Invalid molecular weight");
        var values=new double[4];
        for(int p=0;p<3;p++) {
            if(moles[p].length!=weights.size())throw new IllegalArgumentException("Invalid phase ratio axis");
            if(!Double.isFinite(volumes[p])||volumes[p]<0)throw new IllegalArgumentException("Invalid phase volume");
            for(double amount:moles[p])if(!Double.isFinite(amount)||amount<0)throw new IllegalArgumentException("Invalid phase amount");
            if(this==VOLUME)values[p]=volumes[p];
            else for(int c=0;c<weights.size();c++)values[p]+=moles[p][c]*(this==MASS?weights.get(c):1);
        }
        if(this==VOLUME)values[3]=solids.volume();
        else if(this==MASS)values[3]=solids.massKg();
        else for(var population:solids.populations()) {
            var crystal=population.material().crystal();if(crystal==null)continue;
            int component=components.indexOf(crystal.component());
            if(component<0)throw new IllegalArgumentException("Unknown crystal component");
            values[3]+=population.massKg()/weights.get(component);
        }
        for(double value:values)if(!Double.isFinite(value)||value<0)throw new IllegalArgumentException("Invalid phase total");
        return values;
    }
    public boolean excludesCustomSolids(SolidInventory solids){return this==MOLAR&&solids.populations().stream().anyMatch(p->p.material().crystal()==null);}
}

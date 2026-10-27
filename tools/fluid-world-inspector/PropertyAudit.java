package com.wormzjl.icheme.science.fluid.network;

import com.google.gson.*;
import com.wormzjl.icheme.science.material.MaterialCatalog;
import com.wormzjl.icheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.icheme.science.fluid.network.*;
import com.wormzjl.icheme.science.fluid.state.SolidInventory;
import java.nio.file.*;
import java.util.*;

/** Offline property inspection only: no interval advance, game access, mutation or test framework. */
public final class PropertyAudit {
    static double[] array(JsonElement e){var a=e.getAsJsonArray();double[] v=new double[a.size()];for(int i=0;i<v.length;i++)v[i]=a.get(i).getAsDouble();return v;}
    public static void main(String[] args)throws Exception{
        var root=JsonParser.parseString(Files.readString(Path.of(args[0]))).getAsJsonObject();
        var catalog=MaterialCatalog.bundled();var model=FluidThermodynamics.forNetwork(catalog,"icheme:tjl20_methane_nitrogen",100,1e-6);
        int[] calls={0};Runnable checkpoint=()->{if(++calls[0]>1_000_000)throw new IllegalStateException("Property inspection operation budget");};
        for(var e:root.getAsJsonArray("islands")){
            var island=e.getAsJsonObject();if(island.getAsJsonObject("header").get("id").getAsLong()!=99)continue;
            for(var row:island.getAsJsonObject("graph").getAsJsonArray("nodes")){
                var n=row.getAsJsonObject();long id=n.get("id").getAsLong();if(id==90||id==95)continue;
                var liquid=array(n.get("liquid"));var vapor=array(n.get("vapor"));
                double t=n.get("temperature").getAsDouble(),p=n.get("pressure").getAsDouble(),wl=n.get("waterLiquid").getAsDouble(),wv=n.get("waterVapor").getAsDouble();
                var held=model.state(t,p,liquid,vapor,wl,wv,n.get("hydrocarbonPressure").getAsDouble());
                double[] totals=new double[liquid.length+1];for(int i=0;i<liquid.length;i++)totals[i]=liquid[i]+vapor[i];totals[liquid.length]=wl+wv;
                var stable=model.flashTP(t,p,totals,checkpoint);
                System.out.printf(Locale.ROOT,"node=%d T=%.6f P=%.6f heldMass=%.8f heldVol=%.8f density=%.8f capSpeed=%.8f heldOilMol=%.8f heldGasMol=%.8f TP_oilMol=%.8f TP_gasMol=%.8f TP_volume=%.8f heldH=%.8f TP_H=%.8f%n",id,t,p,held.mass(),held.volume(),held.mass()/held.volume(),model.velocityLimit(held),Arrays.stream(liquid).sum(),Arrays.stream(vapor).sum(),Arrays.stream(stable.liquid()).sum(),Arrays.stream(stable.vapor()).sum(),stable.volume(),held.enthalpy(),stable.enthalpy());
                if(id==30){
                    var uv=FluidThermodynamics.class.getDeclaredMethod("equilibriumUV",double.class,double.class,double.class,double.class,double[].class,Runnable.class);uv.setAccessible(true);
                    var own=(FluidThermodynamics.State)uv.invoke(model,t,p,held.internalEnergy(),held.volume(),totals,checkpoint);
                    System.out.println("VESSEL_UV "+describe(own));
                    var inventory=new PassiveNetwork.Inventory(n.get("capacity").getAsDouble(),totals,held.internalEnergy(),SolidInventory.EMPTY,n.get("height").getAsDouble(),false);
                    int[] record={0};
                    var checked=model.phaseCheck(held,false,held,record,0,null,start->{
                        System.out.println("VESSEL_SPLIT_START "+describe(start));
                        var split=InventoryEquilibrium.split(model,start,inventory,held.volume(),checkpoint);
                        System.out.println("VESSEL_SPLIT_RESULT "+describe(split));return split;
                    },checkpoint);
                    System.out.println("VESSEL_PHASE_CHECK same="+(checked==held)+" refused="+record[0]+" "+describe(checked));
                }
            }
        }
    }
    static String describe(FluidThermodynamics.State s){return s==null?"null":String.format(Locale.ROOT,"T=%.8f P=%.8f oilMol=%.8f gasMol=%.8f volume=%.8f U=%.8f",s.temperature(),s.pressure(),Arrays.stream(s.liquid()).sum(),Arrays.stream(s.vapor()).sum(),s.volume(),s.internalEnergy());}
}

package com.wormzjl.icheme.science.fluid.network;

import java.util.List;

/** Accepted hydraulic row, captured on the solver worker. Observational only; never fed back into the solve. */
public record PipeObservation(long pipe,double firstPressure,double secondPressure,double firstElevation,double secondElevation,
                              double columnDensity,double temperature,boolean flowing,List<Double> lengths,List<Double> losses) {
    public PipeObservation{lengths=List.copyOf(lengths);losses=List.copyOf(losses);if(lengths.isEmpty()||lengths.size()!=losses.size())throw new IllegalArgumentException("Missing observed sections");}
    public static List<PipeObservation> atRest(PassiveNetwork graph,com.wormzjl.icheme.science.fluid.thermo.FluidThermodynamics model){var result=new java.util.ArrayList<PipeObservation>();for(var pipe:graph.pipes()){var a=graph.reservoirs().get(pipe.first());var b=graph.reservoirs().get(pipe.second());double density=PassiveStepSolver.columnDensity(model,a.state(),pipe.firstPort(),pipe.volume());double pa=a.state().pressure()+PassiveStepSolver.portHead(model,a,pipe.firstPort(),a.state()),pb=b.state().pressure()+PassiveStepSolver.portHead(model,b,pipe.secondPort(),b.state());if(!(pipe.control() instanceof FlowControl.Passive))pa=pb+density*PassiveStepSolver.GRAVITY*(b.elevation()-a.elevation());result.add(new PipeObservation(pipe.id(),pa,pb,a.elevation(),b.elevation(),density,a.state().temperature(),false,pipe.sections().stream().map(PipeResistance.Geometry::length).toList(),pipe.sections().stream().map(s->0d).toList()));}return List.copyOf(result);}
    /** Pressure at a tap's hydraulic datum, including the accepted control/port heads and section loss law. */
    public double pressure(double fraction,double elevation){return pressure(fraction,elevation,false);}
    public double pressure(double fraction,double elevation,boolean reversed){double total=lengths.stream().mapToDouble(Double::doubleValue).sum();double distance=Math.clamp(fraction,0,1)*total,passed=0,loss=0,totalLoss=0;for(int i=0;i<lengths.size();i++){int section=reversed?lengths.size()-1-i:i;double length=lengths.get(section),drop=Math.abs(losses.get(section));loss+=drop*Math.clamp((distance-passed)/length,0,1);totalLoss+=drop;passed+=length;}double share=totalLoss>0?loss/totalLoss:Math.clamp(fraction,0,1);double driving=firstPressure-secondPressure-columnDensity*9.80665*(secondElevation-firstElevation);return firstPressure-columnDensity*9.80665*(elevation-firstElevation)-share*driving;}
}

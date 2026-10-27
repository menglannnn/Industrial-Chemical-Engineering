#!/usr/bin/perl -i
# r2 route B experiment: the suction limit's vapour pressure where the seed OR the step's start state holds liquid water.
while(<>){s/if\(!FluidThermodynamics\.holdsPhase\(seed,FluidThermodynamics\.WATER\)\)return edge<suctionFloors/if(!FluidThermodynamics.holdsPhase(seed,FluidThermodynamics.WATER)&&!FluidThermodynamics.holdsPhase(equations.graph.reservoirs().get(a).state(),FluidThermodynamics.WATER))return edge<suctionFloors/;print;}

#!/usr/bin/perl -i
# r2: fix 2 of f983306b off (the TP flash at the mass-weighted temperature), as m22's rule-off-fix2off.patch.
while(<>){s/try\{var split=model\.equilibriumPH\(weighted\/mass,seed\.pressure\(\),enthalpy,n,false,checkpoint\);mixture=model\.folded\(split!=null\?split:model\.flashTP\(weighted\/mass,seed\.pressure\(\),n,checkpoint\),true\);\}/try{mixture=model.folded(model.flashTP(weighted\/mass,seed.pressure(),n,checkpoint),true);}/;print;}

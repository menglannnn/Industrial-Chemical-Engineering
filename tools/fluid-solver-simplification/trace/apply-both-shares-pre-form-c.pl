local $/; binmode STDIN; binmode STDOUT; my $s=<STDIN>; $s =~ s/\r\n/\n/g;
my $n = ($s =~ s/(double\[\] s=phaseAmounts\(start\),e=phaseAmounts\(end\);double ts=sum\(s\),te=sum\(e\);int bits=0;\n\s*for\(int k=0;k<4;k\+\+\)if\()e\[k\]<VANISHING_SHARE\*te&&/${1}e[k]<VANISHING_SHARE*te&&ev[k]<VANISHING_SHARE*tv&&/);
$n += ($s =~ s/(double\[\] s=phaseAmounts\(start\),e=phaseAmounts\(end\);double ts=sum\(s\),te=sum\(e\);int bits=0;\n)/$1        double[] ev=phaseVolumes(end);double tv=sum(ev);\n/);
my $add = "    private static double[] phaseVolumes(State state) {\n        double hv=sum(state.vaporView()),wv=state.waterVapor(),gas=hv+wv,vv=state.vaporVolume();\n        return new double[]{state.liquidVolume(),gas>0?vv*hv/gas:0,state.waterVolume(),gas>0?vv*wv/gas:0};\n    }\n";
$n += ($s =~ s/(    \/\*\* A state's fluid amount by phase)/$add$1/);
print STDERR "edits $n\n"; print $s;

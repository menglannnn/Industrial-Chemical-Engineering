#!/usr/bin/perl
# experiment E5 (threshold option, owner decision): a hydrocarbon liquid (phase bit 0) is nothing / vanishing below VANISHING_SHARE by volume alone.
use strict; my $root=shift or die; my $f="$root/src/main/java/com/wormzjl/createcheme/science/fluid/thermo/FluidThermodynamics.java";
local @ARGV=($f); local $^I=""; my $n=0;
while(<>){ s/\x0d\x0a$/\x0a/;
  $n+=s/if\(e\[k\]<VANISHING_SHARE\*te&&ev\[k\]<VANISHING_SHARE\*tv&&e\[k\]\*ts<s\[k\]\*te\)/if((k==0||e[k]<VANISHING_SHARE*te)&&ev[k]<VANISHING_SHARE*tv&&e[k]*ts<s[k]*te)/;
  $n+=s/if\(e\[k\]>0&&e\[k\]<VANISHING_SHARE\*te&&ev\[k\]<VANISHING_SHARE\*tv\)/if(e[k]>0&&(k==0||e[k]<VANISHING_SHARE*te)&&ev[k]<VANISHING_SHARE*tv)/;
  print; }
die "volumeshare: $n edits\n" unless $n==2;

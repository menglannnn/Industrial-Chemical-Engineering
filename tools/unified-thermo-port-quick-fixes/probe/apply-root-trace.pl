#!/usr/bin/env perl
# Trace for the X class (no physical EOS root): adds the reduced parameters, the discriminant and the computed roots to
# the kernel's IllegalStateException message, and prints the network phase evaluation that met it (T, P, root, amounts)
# as a QFROOT line before rethrowing. Applied to an exported tree only: perl apply-root-trace.pl /tmp/forms-<name>
use strict; use warnings;
my $d = shift or die "usage: apply-root-trace.pl <tree>";
sub edit { my ($f, $from, $to) = @_; local $/; open my $h, '<', $f or die "$f: $!"; my $s = <$h>; close $h;
  my $n = ($s =~ s/\Q$from\E/$to/); die "pattern not found in $f: $from" unless $n; open $h, '>', $f or die; print $h $s; close $h; }
my $k = "$d/src/main/java/com/wormzjl/createcheme/science/thermo/PengRobinsonKernel.java";
edit($k, 'throw new IllegalStateException("Peng-Robinson equation has no physical " + root + " root");',
  'throw new IllegalStateException("Peng-Robinson equation has no physical " + root + " root"'
  . ' + " A=" + reducedA + " B=" + reducedB + " D=" + discriminant + " largest=" + largest + " smallest=" + smallestPhysical + " p=" + p + " q=" + q);');
my $m = "$d/src/main/java/com/wormzjl/createcheme/science/fluid/thermo/HydrocarbonModel.java";
edit($m, 'var values=translated.evaluateValues(t,p,amounts,root,terms);',
  'TranslatedPengRobinson.Values values;try{values=translated.evaluateValues(t,p,amounts,root,terms);}catch(IllegalStateException e){'
  . 'System.out.println("QFROOT root="+root+" t="+t+" p="+p+" vaporCoexists="+vaporCoexists+" amounts="+java.util.Arrays.toString(amounts)+" msg="+e.getMessage());throw e;}');
print "applied root trace to $d\n";

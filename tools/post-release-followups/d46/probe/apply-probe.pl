#!/usr/bin/perl
# t52: applies the D46 probe to a lane export (never the worktree) and writes probe copies of the two hold-fuzz harnesses.
# Usage: perl apply-probe.pl <export dir> <worktree> <out dir for harness copies>
use strict; use warnings; use File::Copy;
my ($export,$wt,$out)=@ARGV; die "usage: apply-probe.pl <export> <worktree> <harness out dir>" unless $out;
my $here=$0; $here=~s{[\\/][^\\/]+$}{};
my $pkg="$export/src/main/java/com/wormzjl/createcheme/runtime/fluid";
copy("$here/D46Probe.java","$pkg/D46Probe.java") or die "copy: $!";
# Each pair: a pattern and a closure building the replacement from the captures.
sub edit { my ($f,@subs)=@_; open my $in,"<",$f or die "$f: $!"; local $/; my $s=<$in>; close $in; my $crlf=$s=~/\r\n/; $s=~s/\r\n/\n/g;
  while(@subs){my ($re,$rep)=splice(@subs,0,2); my $n=($s=~s/$re/$rep->()/e); die "no match for $re in $f" unless $n==1;}
  $s=~s/\n/\r\n/g if $crlf; open my $o,">",$f or die; print $o $s; close $o; }
edit("$pkg/IslandCoordinator.java",
  qr/(                    island\.graph=candidate\.graph\(\);island\.lastResult=Optional\.of\(candidate\);)/,
  sub{"                    D46Probe.committed(island.graph,candidate.graph(),candidate.boundaries(),island.model);\n$1"},
  qr/(            island\.clock\.rest\(target,island\.fence\(\)\);)(island\.graph=replay\.graph\(\);)/,
  sub{"$1D46Probe.committed(island.graph,replay.graph(),null,island.model);$2"},
  qr/(            var island=new Island\(new Snapshot\(replacement\.id,revision,graph,[^\n]*\n)/,
  sub{"$1            D46Probe.installed(graph,model);\n"});
print "probe applied to $export\n";
mkdir $out; for my $d ("cold","bundled") { mkdir "$out/$d";
  my $src="$wt/tools/unified-thermo-port-forms/hold-fuzz-$d/src/HoldFuzzTest.java"; my $dst="$out/$d/HoldFuzzTest.java";
  copy($src,$dst) or die "copy $src: $!";
  edit($dst, qr/(        World w=null;String islands="";\n)/, sub{"$1        D46Probe.reset();\n"},
             qr/(        return new Outcome\(seed,placements,)/, sub{"        D46Probe.flush(seed);\n$1"});
  print "harness copy $dst\n"; }

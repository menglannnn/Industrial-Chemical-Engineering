# As d40/notime.pl, also blanking allocation fields (bytes=, allocatedMB=, allocated MB) and wall-clock words:
# which MOVED blocks of a compare-test-output.pl report differ only in timing or allocation?
my ($t,@m,@p);
sub norm { my $s=shift; $s=~s/\bms=\d+//g; $s=~s/[\d.]+ ms\b//g; $s=~s/^\[\d\d:\d\d:\d\d\]//; $s=~s/\{(\d+=[\d.]+(, )?)+\}//g if $s=~/median ms/;
  $s=~s/bytes=\d+//g; $s=~s/allocatedMB=[\d.]+//g; $s=~s/\.java:\d+\)/.java)/g; $s=~s/allocated MB[^|]*\|//g; $s=~s/wall ms[^|]*\|//g; $s=~s/wallMs=\d+//g; $s=~s/seedMsSum=\d+//g;
  $s=~s/\b(nanos|Nanos|millis|Millis|seconds)=\d+//g; $s=~s/\b\d+(\.\d+)? (ms|s|ns|us)\b//g; return $s }
sub flush { return unless defined $t; my $same=@m==@p; for my $i (0..$#m){ $same=0 if norm($m[$i]) ne norm($p[$i]//'') } printf "%s %s\n",($same?"TIMING-ONLY":"PHYSICS    "),$t }
while(<>){ chomp; s/\r$//; if(/^MOVED (.*)/){flush(); $t=$1; @m=(); @p=(); next} if(/^  - (.*)/){push @m,$1} elsif(/^  \+ (.*)/){push @p,$1} elsif(/^(tests compared|NEW ONLY|BASELINE ONLY)/){flush(); undef $t} }
flush();

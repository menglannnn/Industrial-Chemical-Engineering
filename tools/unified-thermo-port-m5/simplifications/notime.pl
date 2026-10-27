# Which MOVED blocks differ only in wall-clock fields (ms=N, "N ms", [hh:mm:ss], median ms maps)?
my ($t,@m,@p);
sub norm { my $s=shift; $s=~s/\bms=\d+//g; $s=~s/[\d.]+ ms\b//g; $s=~s/^\[\d\d:\d\d:\d\d\]//; $s=~s/\{(\d+=[\d.]+(, )?)+\}//g if $s=~/median ms/; $s=~s/bytes=\d+//g; return $s }
sub flush { return unless defined $t; my $same=@m==@p; for my $i (0..$#m){ $same=0 if norm($m[$i]) ne norm($p[$i]//'') } printf "%s %s\n",($same?"TIMING-ONLY":"PHYSICS    "),$t }
while(<>){ chomp; s/\r$//; if(/^MOVED (.*)/){flush(); $t=$1; @m=(); @p=(); next} if(/^  - (.*)/){push @m,$1} elsif(/^  \+ (.*)/){push @p,$1} elsif(/^(tests compared|NEW ONLY|OLD ONLY)/){flush(); undef $t} }
flush();

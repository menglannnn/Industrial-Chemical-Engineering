# For each MOVED block of compare-test-output.pl: pair "-" and "+" lines in order, compare their numbers token by token;
# print the largest relative change, the line count, and whether any non-numeric token changed.
my ($t,@m,@p);
sub flush { return unless defined $t; my ($max,$where,$text)=(0,'',0);
  for my $i (0..$#m) { my @a=split /([-+]?\d+\.?\d*(?:[eE][-+]?\d+)?)/,$m[$i]; my @b=split /([-+]?\d+\.?\d*(?:[eE][-+]?\d+)?)/,($p[$i]//'');
    if(@a!=@b){$text++;next}
    for my $k (0..$#a){ if($k%2){ my ($x,$y)=($a[$k],$b[$k]); my $d=abs($x-$y)/(abs($x)>abs($y)?(abs($x)||1e-300):(abs($y)||1e-300)); if($d>$max){$max=$d;$where=substr($a[$k-1],-24)."$x -> $y"} } elsif($a[$k] ne $b[$k]){$text++} } }
  printf "%-110s lines=%d maxRel=%.3g text=%d at: %s\n",$t,scalar(@m),$max,$text,$where; }
while(<>){ chomp; s/\r$//; if(/^MOVED (.*)/){flush(); $t=$1; @m=(); @p=(); next} if(/^  - (.*)/){push @m,$1} elsif(/^  \+ (.*)/){push @p,$1} elsif(/^(tests compared|NEW ONLY|OLD ONLY)/){flush(); undef $t} }
flush();

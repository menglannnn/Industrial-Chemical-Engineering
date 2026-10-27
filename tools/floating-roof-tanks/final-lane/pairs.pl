# pairs.pl A B: for each masked line key whose lines differ, the first differing pair (A then B), numbers kept.
my ($a,$b)=@ARGV; sub load{my %h;my @o;open(my $f,"<",shift);while(<$f>){chomp;(my $k=$_)=~s/-?\d+\.?\d*(?:[eE][-+]?\d+)?/#/g;push @o,$k unless $h{$k};push @{$h{$k}},$_;}(\%h,\@o)}
my ($A,$oa)=load($a);my ($B)=load($b);
for my $k(@$oa){next unless $B->{$k};my @x=@{$A->{$k}};my @y=@{$B->{$k}};for my $i(0..($#x<$#y?$#x:$#y)){if($x[$i] ne $y[$i]){print "A ",substr($x[$i],0,330),"\nB ",substr($y[$i],0,330),"\n\n";last}}}

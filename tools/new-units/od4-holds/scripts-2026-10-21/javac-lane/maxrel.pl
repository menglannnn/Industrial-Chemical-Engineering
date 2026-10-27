# pairs printed lines of two runs by their text with numbers masked; prints the largest relative move per line key
my ($a,$b)=@ARGV; sub load{my %h;open(my $f,"<",shift);while(<$f>){next if /^M3RUN/;chomp;(my $k=$_)=~s/-?\d+\.?\d*(?:[eE][-+]?\d+)?/#/g;push @{$h{$k}},$_;}%h}
my %A=load($a);my %B=load($b);my %worst;
for my $k(keys %A){next unless $B{$k};my @x=@{$A{$k}};my @y=@{$B{$k}};for my $i(0..($#x<$#y?$#x:$#y)){my @p=$x[$i]=~/-?\d+\.?\d*(?:[eE][-+]?\d+)?/g;my @q=$y[$i]=~/-?\d+\.?\d*(?:[eE][-+]?\d+)?/g;for my $j(0..$#p){my $d=abs($p[$j]-$q[$j]);my $s=abs($p[$j])>abs($q[$j])?abs($p[$j]):abs($q[$j]);my $r=$s>0?$d/$s:0;my $t=substr($k,0,40);$worst{$t}=$r if !defined $worst{$t}||$r>$worst{$t};}}}
for my $t(sort{$worst{$b}<=>$worst{$a}}keys %worst){printf "%.2e %s\n",$worst{$t},$t if $worst{$t}>0;}

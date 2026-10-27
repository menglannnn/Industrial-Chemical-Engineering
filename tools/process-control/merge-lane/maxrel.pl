# Pairs the i-th removed and added line of one class from a diff and prints the largest relative move of numbers above 1e-6.
my ($class,$file)=@ARGV; my (@a,@b);
open my $f,'<',$file or die; while(<$f>){ push @a,$1 if /^< \Q$class\E \| (.*)/; push @b,$1 if /^> \Q$class\E \| (.*)/; }
my ($max,$where)=(0,""); my $mism=0;
for my $i (0..$#a){ my @x=$a[$i]=~/(-?\d+\.?\d*(?:E-?\d+)?)/gi; my @y=($b[$i]//"")=~/(-?\d+\.?\d*(?:E-?\d+)?)/gi; if(@x!=@y){$mism++;next;}
  for my $j (0..$#x){ my ($u,$v)=($x[$j],$y[$j]); next if abs($u)<1e-6&&abs($v)<1e-6; my $r=abs($u-$v)/(abs($u)>abs($v)?abs($u):abs($v)); if($r>$max){$max=$r;$where=substr($a[$i],0,60)." [$u -> $v]";} } }
printf "%s pairs=%d shape-mismatch=%d max-rel=%.3g at %s\n",$class,scalar(@a),$mism,$max,$where;

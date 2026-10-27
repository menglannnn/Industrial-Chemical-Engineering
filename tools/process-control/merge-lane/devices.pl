# Lists every "new Device(" / "new PhysicalFluidTopology.Device(" call with its top-level argument count.
while(my $line=<>){
  my $l=$line; chomp $l;
  while($l=~/new (?:PhysicalFluidTopology\.)?Device\(/g){
    my $p=pos($l); my $d=1; my $i=$p; my $c=0;
    while($i<length($l) && $d>0){ my $ch=substr($l,$i,1); $d++ if $ch eq "("; $d-- if $ch eq ")"; $c++ if $ch eq "," && $d==1; $i++; }
    my $args=substr($l,$p,$i-$p-1);
    (my $f=$ARGV)=~s#src/main/java/com/wormzjl/icheme/##;
    printf "%s:%d args=%d %s\n",$f,$.,$c+1,substr($args,0,150);
  }
} continue { close ARGV if eof; }

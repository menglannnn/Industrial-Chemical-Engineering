# perl perseed.pl base-report new-report [exclude,...] : per-seed counter sums (COUNTERS_SEED, one worker) and the movers
my ($fa,$fb,$ex)=@ARGV; my %ex=map{$_=>1} split /,/, ($ex//'');
sub rd{my($f)=@_;my(%c,$s);open my $F,'<',$f or die;while(<$F>){ if(/^SEED (\d+)/){$s=$1} elsif(/COUNTERS_SEED(.*)/){ my %h=($1=~/(\w+)=(\d+)/g); $c{$s}=\%h; } } \%c}
my $A=rd($fa); my $B=rd($fb); my @k=qw(newtonIterations newtonSolves stepAttempts);
my(%sa,%sb,@d);
for my $s (keys %$A){ next if $ex{$s}; next unless $B->{$s}; for(@k){$sa{$_}+=$A->{$s}{$_}//0;$sb{$_}+=$B->{$s}{$_}//0}; push @d,[$s,($B->{$s}{newtonIterations}//0)-($A->{$s}{newtonIterations}//0),$A->{$s}{newtonIterations},$B->{$s}{newtonIterations},$A->{$s}{stepAttempts},$B->{$s}{stepAttempts}] }
printf "seeds %d\n",scalar(@d);
for(@k){printf "%-18s base %8d new %8d  %+6.1f%%\n",$_,$sa{$_},$sb{$_},100*($sb{$_}-$sa{$_})/($sa{$_}||1)}
@d=sort{$b->[1]<=>$a->[1]}@d; print "largest rises (seed dIter iterBase iterNew stepsBase stepsNew):\n"; printf "  %s\n", join(' ',@$_) for @d[0..11];
print "largest falls:\n"; printf "  %s\n", join(' ',@$_) for @d[-6..-1];
my $tot=0; $tot+=$_->[1] for @d; my $top=0; $top+=$d[$_][1] for 0..9; my $up=0; $up++ for grep{$_->[1]>0}@d; my $dn=0; $dn++ for grep{$_->[1]<0}@d;
printf "total dIter %d, top-10 rises %d; seeds up %d, down %d, equal %d\n",$tot,$top,$up,$dn,scalar(@d)-$up-$dn;

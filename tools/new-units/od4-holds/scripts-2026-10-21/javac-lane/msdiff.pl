# per-seed ms of two campaign reports, seeds in both, largest rises first
sub l{my %m;open my $h,"<",shift or die;while(<$h>){$m{$1}=$2 if /^SEED (\d+) blocks=\d+ ms=(\d+) /}\%m}
my($A,$B)=(l($ARGV[0]),l($ARGV[1]));my @r;
for my $s(keys %$A){next unless exists $B->{$s};push @r,[$s,$A->{$s},$B->{$s},$B->{$s}-$A->{$s}]}
printf "%s %d -> %d (%+d)\n",@$_ for (sort{$b->[3]<=>$a->[3]}@r)[0..7];

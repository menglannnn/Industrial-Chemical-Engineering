# Prints every captured stdout line of the JUnit XML files given, as "SimpleClass | line" (the final lane's printed lines).
for my $file (@ARGV){
  open my $f,'<',$file or die "$file: $!"; local $/; my $x=<$f>; close $f;
  my ($suite)=$x=~/<testsuite name="([^"]+)"/; next unless $suite; (my $simple=$suite)=~s/.*\.//;
  while($x=~/<system-out><!\[CDATA\[(.*?)\]\]><\/system-out>/sg){ for my $l (split /\r?\n/,$1){ next if $l=~/^\s*$/; print "$simple | $l\n"; } }
}

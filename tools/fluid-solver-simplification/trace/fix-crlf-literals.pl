local $/; binmode STDIN; binmode STDOUT; my $s = <STDIN>;
my $n = ($s =~ s/'\r?\n'\)/(char)10)/g);
$n += ($s =~ s/\("\)\r?\n"\);/(")"+(char)10);/g);
print STDERR "fixed $n\n"; print $s;

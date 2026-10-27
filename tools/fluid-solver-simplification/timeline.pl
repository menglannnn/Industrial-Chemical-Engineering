# Compact timeline of a t38 trace: one line per TRACE header with the reservoirs and edges that follow it.
my @buf; my $h;
sub flush { return unless defined $h; my $line=$h; $line =~ s/ modes=.*?proposal=/ proposal=/; my @n=(); my @e=();
  for (@buf) { if (/kind=RESERVOIR .*?(P=\S+ T=\S+ m=\S+ liqV=\S+ vapV=\S+)/) { push @n, $1 } elsif (/edge (\d+) .*?(y=\S+ cap=\S+ q=\S+ slope=\S+)/) { push @e, "e$1 $2" } }
  print "$line | ", join(" | ", @n, @e), "\n"; @buf=(); undef $h; }
while (<>) { chomp; if (/^TRACE/) { flush(); $h=$_; next } if (defined $h && /^  (node|edge)/) { push @buf, $_ } }
flush();

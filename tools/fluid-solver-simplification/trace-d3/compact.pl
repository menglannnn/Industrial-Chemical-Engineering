# One line per TRACE header (pass/converged/fail): dt, every node's P (kPa), every edge's q and y/cap; T40STEP lines pass through.
my @buf; my $h;
sub flush { return unless defined $h; my ($what,$pass,$dt)=$h=~/^TRACE (\w+) pass=(\d+) dt=(\S+)/; my @n; my @e;
  for (@buf) { if (/node (\d+) id=(\d+) kind=(\w)\w* z=\S+ P=(\S+)/) { push @n, sprintf("n%d=%.1f",$1,$4/1000) } elsif (/edge (\d+) .*?y=(\S+) cap=(\S+) q=(\S+)/) { push @e, sprintf("e%d=%.3f%s",$1,$4,($3 ne 'Infinity' && abs($4)>0.98*$3)?"*":"") } }
  printf "%-9s p%s dt=%.4g | %s | %s\n",$what,$pass,$dt,join(" ",@n),join(" ",@e); @buf=(); undef $h; }
while (<>) { chomp; if (/^TRACE/) { flush(); $h=$_; next } if (/^T40STEP/) { flush(); print substr($_,0,200),"\n"; next } if (defined $h && /^  (node|edge)/) { push @buf, $_ } }
flush();

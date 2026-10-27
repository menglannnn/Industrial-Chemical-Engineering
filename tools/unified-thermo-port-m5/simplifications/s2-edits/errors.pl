# Summarises a javac log: error count per file (first argument "files"), or every error line with its message
# (first argument "lines"); the log is read from the remaining arguments or stdin.
my $mode=shift; my %count; my @lines;
while(<>){ s/\r$//; if(m{([^/\\]+\.java):(\d+): error: (.*)}){ $count{$1}++; push @lines,"$1:$2: $3"; } }
if($mode eq 'files'){ printf "%4d %s\n",$count{$_},$_ for sort{$count{$b}<=>$count{$a}||$a cmp $b} keys %count; }
else { print "$_\n" for @lines; }

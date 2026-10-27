# Mechanical restatement of the test sources for S2 (owner decision D37): the acceptance flag, the fallback allowance
# and the approximation anchor are gone. Edits each named file in place; prints the number of substitutions per file.
use strict; use warnings;
for my $file (@ARGV) {
    open my $in,'<:raw',$file or die "$file: $!"; local $/; my $s=<$in>; close $in; my $n=0;
    # A vacuous assertion on a line of its own, then inline.
    $n+=($s=~s/^[ \t]*assertEquals\(PassiveStepSolver\.Acceptance\.FULL,\w+\.acceptance\(\)(?:,[^;\n]*?)?\);[ \t]*\r?\n//mg);
    $n+=($s=~s/assertEquals\(PassiveStepSolver\.Acceptance\.FULL,\w+\.acceptance\(\)(?:,[^;\n]*?)?\);//g);
    # The interval constructor's acceptance argument.
    $n+=($s=~s/,(\s*)(?:result\.acceptance\(\)|PassiveStepSolver\.Acceptance\.FULL),(\s*)(List\.of\(new PipeTransfer|result\.pipeTransfers\(\))/,$1$3/g);
    $n+=($s=~s/,PassiveStepSolver\.Acceptance\.FULL,PassiveIntervalSolver\.StageGuard\./,PassiveIntervalSolver.StageGuard./g);
    # Snapshots: no allowance and no anchor.
    $n+=($s=~s/(?:com\.wormzjl\.createcheme\.runtime\.fluid\.)?FallbackAllowance\.NONE,(\s*)(?:java\.util\.)?Optional\.empty\(\),/$1/g);
    $n+=($s=~s/,s\.allowance\(\),s\.anchor\(\),/,/g);
    # Worker results: no proposed allowance or anchor.
    $n+=($s=~s/,FallbackAllowance\.NONE,Optional\.of\(ApproximationAnchor\.fromFull\(\w+,\w+\)\)\)/)/g);
    $n+=($s=~s/,FallbackAllowance\.NONE,Optional\.empty\(\)\)/)/g);
    # The model's identity strings.
    $n+=($s=~s/ApproximationAnchor\.revision\(([\w.()]+?)\)/$1.revision()/g);
    $n+=($s=~s/ApproximationAnchor\.thermodynamicRevision\(([\w.()]+?)\)/$1.thermodynamicRevision()/g);
    $n+=($s=~s/^import com\.wormzjl\.createcheme\.science\.fluid\.network\.ApproximationAnchor;\r?\n//mg);
    $n+=($s=~s/^import com\.wormzjl\.createcheme\.runtime\.fluid\.FallbackAllowance;\r?\n//mg);
    $n+=($s=~s/SolidMobility\.requiresFull\(/SolidMobility.closable(/g);
    if($n){ open my $out,'>:raw',$file or die; print $out $s; close $out; }
    printf "%3d %s\n",$n,$file;
}

#!/usr/bin/perl -i
# r8 probe (850): why portRate fails (the exception's class and message). Export-only, PassiveStepSolver.java.
while(<>){
  if(/^(\s*)catch\(SparseNewton\.Nonconvergence\|IllegalArgumentException failed\)\{return null;\}\s*$/){
    my $i=$1; print $i.'catch(SparseNewton.Nonconvergence|IllegalArgumentException failed){System.out.println("S850 RATEFAIL "+failed.getClass().getSimpleName()+": "+String.valueOf(failed.getMessage()).replace(\'\n\',\' \'));return null;}'."\n"; next;
  }
  print;
}

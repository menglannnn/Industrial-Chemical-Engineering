#!/usr/bin/env bash
# t28: commits code without the instrumentation. Reverses the pass-trace patch, removes T19Trace.java, refuses if any trace
# or scratch switch is left in src/, stages the named paths and commits with the message file, retrying while another
# process holds the worktree's index.lock (it is transient here), then applies the trace patch again.
# From the worktree root. Usage: t28-commit.sh <message file> <path>...
set -u
P=tools/phase-ports-probes/hold-classes/src/instr-t28-pass-trace-on-84787c80.patch; msg=$1; shift
retry(){ for i in $(seq 1 30); do "$@" 2>/tmp/t28-git-err.txt && return 0; grep -q "index.lock" /tmp/t28-git-err.txt || { cat /tmp/t28-git-err.txt; return 1; }; sleep 2; done; cat /tmp/t28-git-err.txt; return 1; }
traced=0; [ -f src/main/java/com/wormzjl/createcheme/science/fluid/network/T19Trace.java ] && traced=1
if [ $traced = 1 ]; then git apply -R $P || exit 1; rm -f src/main/java/com/wormzjl/createcheme/science/fluid/network/T19Trace.java; fi
if grep -rn "T19Trace\|T28SCRATCH\|t28off" src/main src/test; then echo "instrumentation left; not committing"; rc=1
else retry git add -- "$@" && retry git commit -q -F "$msg" && git log --oneline -1; rc=$?; fi
[ $traced = 1 ] && git apply $P && echo "trace re-applied"
exit $rc

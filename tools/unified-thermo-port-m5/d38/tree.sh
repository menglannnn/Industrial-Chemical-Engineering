#!/usr/bin/env bash
# Prints a git tree object of the worktree's current src/ (a temporary index; the real index, HEAD and the stash are
# untouched), for lane.sh export: bash lane.sh export <name> "$(bash tree.sh)"
set -eu
WT=${WT:-D:/Minecraft/Modding/1.21/CreateChemE/.claude/worktrees/coolprop-multiphase-thermo-37f6b0}
I=$(mktemp -u /tmp/d38-index.XXXXXX)
GIT_INDEX_FILE=$I git -C "$WT" read-tree HEAD
GIT_INDEX_FILE=$I git -C "$WT" add -A src
GIT_INDEX_FILE=$I git -C "$WT" write-tree
rm -f "$I"

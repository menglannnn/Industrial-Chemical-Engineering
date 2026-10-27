# Pipe flange spacing review

**Date:** 2026-10-24  
**Branch:** `claude/floating-roof-tanks-81d68b`  
**Status:** implemented in `9ab5f6cf`; development client loaded a world.

Owner request: double the straight-pipe distance between flanged spool ends.

`FluidDeviceBlockEntity.SPOOL` changes from 6 to 12 blocks. The existing `spoolEnd` coordinate rule and flange geometry are unchanged.

Verification: `compileJava` and `git diff --check` pass. No test, save, protocol or physics change.

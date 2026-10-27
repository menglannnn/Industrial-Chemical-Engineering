# Deferred fluid hold evidence plan

2026-10-23. Owner reported another hold while playing and explicitly requested recording it for later debugging.

- Freeze the current client/server logs, atomic current-format fluid core and immutable pack bytes, source revision/local patch, config and file hashes without changing the running world.
- Validate the copied format-17/unit-10 unit references by SHA-256. Record save timestamp, online epoch and available island clocks separately from the live log's newer hold timestamps.
- Capture the next normal autosave if available while documentation is being written. Never force a solve, input, save, pause, restart or process shutdown.
- Record exact hold messages and case limitations. Defer root-cause tracing, reproduction and code changes until the owner resumes debugging. Add no migration, legacy fixture, test suite, campaign or automation.

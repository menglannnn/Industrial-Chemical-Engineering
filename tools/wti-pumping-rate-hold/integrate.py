"""Preserve the explicitly requested integration worktree's local files and fast-forward.

Run capture before integration, then merge only after final checks finish. The tool
does not stage, stash, commit or write any of the target's local equipment/test files.
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import validate_fix as campaign

target = campaign.MAIN / ".claude/worktrees/rename-ichem-mod-30e50c"
evidence = campaign.HERE / "integration"
evidence.mkdir(exist_ok=True)


def git(*args):
    return subprocess.check_output(["git", *args], cwd=target, text=True, encoding="utf-8").strip()


def files():
    entries = subprocess.check_output(["git", "status", "--porcelain", "-z"], cwd=target).decode("utf-8").split("\0")
    answer = {}
    for entry in entries:
        if not entry: continue
        assert entry[:2] in (" M", "??"), entry
        name = entry[3:]
        path = (target / name).resolve()
        assert path.is_relative_to(target.resolve()) and path.is_file(), name
        answer[name] = {"sha256": hashlib.sha256(path.read_bytes()).hexdigest(), "status": entry[:2]}
    return answer


parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("phase", choices=("capture", "merge"))
args = parser.parse_args()
assert git("branch", "--show-current") == "claude/floating-roof-tanks-81d68b"
if args.phase == "capture":
    current = {"target": str(target), "branch": git("branch", "--show-current"), "head": git("rev-parse", "HEAD"), "files": files()}
    (evidence / "before.json").write_text(json.dumps(current, indent=2))
    print("Preserved manifest:", len(current["files"]), "local files at", current["head"])
else:
    before = json.loads((evidence / "before.json").read_text())
    assert git("rev-parse", "HEAD") == before["head"]
    assert files() == before["files"], "Local files changed after capture; recapture before merging."
    assert not git("ls-files", "-i", "-c", "--exclude-standard")
    source = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=campaign.WT, text=True).strip()
    overlap = set(git("diff", "--name-only", before["head"], source).splitlines()) & set(before["files"])
    assert not overlap, overlap
    with (evidence / "merge.log").open("w", encoding="utf-8") as log:
        subprocess.run(["git", "merge", "--ff-only", source], cwd=target, stdout=log, stderr=subprocess.STDOUT, check=True)
    assert git("rev-parse", "HEAD") == source
    assert files() == before["files"], "Preservation check failed."
    assert not git("ls-files", "-i", "-c", "--exclude-standard")
    after = {"head": source, "files": files(), "preserved": True}
    (evidence / "after.json").write_text(json.dumps(after, indent=2))
    print("MERGE_DONE", source, "all", len(after["files"]), "local files preserved")

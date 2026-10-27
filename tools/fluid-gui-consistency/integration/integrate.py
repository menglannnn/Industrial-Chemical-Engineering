"""Fast-forward the requested integration branch only when no incoming path overlaps local edits.

prepare freezes HEAD/source/status/hashes; apply refuses any intervening change.
No stash, reset, staging or local-file rewriting is used.
"""
from pathlib import Path
import hashlib
import json
import subprocess
import sys

HERE = Path(__file__).resolve().parent
TARGET = Path('D:/Minecraft/Modding/1.21/CreateChemE/.claude/worktrees/rename-ichem-mod-30e50c')
OWN = Path('C:/Users/wormz/.codex/worktrees/pipeline-tool/CreateChemE')


def git(*args, cwd=TARGET):
    return subprocess.check_output(['git', *args], cwd=cwd, stderr=subprocess.STDOUT)


def files():
    result = {}
    for entry in git('status', '--porcelain', '-z').decode('utf-8').split('\0'):
        if not entry:
            continue
        assert entry[:2] in (' M', '??'), entry
        name = entry[3:]
        path = (TARGET / name).resolve()
        assert path.is_relative_to(TARGET.resolve()) and path.is_file(), name
        result[name] = {'status': entry[:2], 'sha256': hashlib.sha256(path.read_bytes()).hexdigest()}
    return result


def snapshot():
    assert git('branch', '--show-current').decode().strip() == 'claude/floating-roof-tanks-81d68b'
    assert not git('diff', '--cached', '--name-only').strip()
    assert not git('ls-files', '-i', '-c', '--exclude-standard').strip()
    assert not git('status', '--porcelain', cwd=OWN).strip()
    return {'target': str(TARGET), 'head': git('rev-parse', 'HEAD').decode().strip(),
            'incoming': git('rev-parse', 'HEAD', cwd=OWN).decode().strip(), 'files': files()}


if sys.argv[1:] == ['prepare']:
    before = snapshot()
    assert before['head'].startswith('b52732c9') and before['incoming'].startswith('35bad9b8')
    changed = set(git('diff', '--name-only', before['head'], before['incoming']).decode().splitlines())
    assert not changed.intersection(before['files']), 'Incoming change overlaps owner files'
    assert len(before['files']) == 10
    (HERE / 'before.json').write_text(json.dumps(before, indent=2) + '\n', encoding='utf-8')
    print('PREFLIGHT: ten local files, no incoming overlap')
elif sys.argv[1:] == ['apply']:
    before = json.loads((HERE / 'before.json').read_text(encoding='utf-8'))
    assert snapshot() == before, 'Integration/source/local files changed; repeat preflight'
    git('merge-base', '--is-ancestor', before['head'], before['incoming'])
    log = git('merge', '--ff-only', before['incoming'])
    (HERE / 'merge.log').write_bytes(log)
    after = snapshot()
    assert after['head'] == before['incoming']
    assert after['files'] == before['files'], 'Owner files changed'
    (HERE / 'after.json').write_text(json.dumps(after, indent=2) + '\n', encoding='utf-8')
    print('MERGED:', after['head'], 'all ten local files byte-for-byte preserved')
else:
    raise SystemExit('Use prepare or apply')

"""Integrate the approved feature while preserving target WIP, including one disjoint overlapping file.

before.json and ProcessUnit.expected.java come from the read-only three-way preflight.
Only the overlapping file uses a named stash; it is reapplied by exact object ID and
retained as a recovery backup. No local edits are committed to the target branch.
"""
from pathlib import Path
import hashlib,json,subprocess

HERE=Path(__file__).resolve().parent/'integration'
OWN=Path('C:/Users/wormz/.codex/worktrees/pipeline-tool/CreateChemE')
before=json.loads((HERE/'before.json').read_text(encoding='utf-8'))
TARGET=Path(before['target'])

def git(*args,cwd=TARGET):
    return subprocess.check_output(['git',*args],cwd=cwd,stderr=subprocess.STDOUT)

def files():
    out={}
    for entry in git('status','--porcelain','-z').decode('utf-8').split('\0'):
        if not entry:continue
        assert entry[:2] in (' M','??'),entry
        name=entry[3:];p=(TARGET/name).resolve()
        assert p.is_relative_to(TARGET.resolve()) and p.is_file(),name
        out[name]={'status':entry[:2],'sha256':hashlib.sha256(p.read_bytes()).hexdigest()}
    return out

assert git('branch','--show-current').decode().strip()==before['branch']=='claude/floating-roof-tanks-81d68b'
assert git('rev-parse','HEAD').decode().strip()==before['head'],'Target HEAD changed'
assert git('rev-parse','HEAD',cwd=OWN).decode().strip()==before['incoming'],'Source HEAD changed'
assert files()==before['files'],'Target local files changed; repeat preflight'
assert not git('ls-files','-i','-c','--exclude-standard').strip()
assert not git('diff','--cached','--name-only').strip()
path=before['overlap']
assert set(git('diff','--name-only',before['head'],before['incoming']).decode().splitlines())&set(before['files'])=={path}
stash_name='codex-pressure-valve-merge-2026-10-24-ProcessUnit-local-edits'
log=[]
log.append(git('stash','push','--message',stash_name,'--',path).decode('utf-8'))
stash=git('rev-parse','refs/stash').decode().strip()
(HERE/'stash.json').write_text(json.dumps({'name':stash_name,'object':stash,'path':path,'restored':False},indent=2),encoding='utf-8')
remaining=files();assert set(remaining)==set(before['files'])-{path}
for name,value in remaining.items():assert value==before['files'][name],name
applied=False
try:
    log.append(git('merge','--ff-only',before['incoming']).decode('utf-8'))
    log.append(git('stash','apply',stash).decode('utf-8'));applied=True
except BaseException:
    if not applied and git('rev-parse','HEAD').decode().strip()==before['head']:
        log.append(git('stash','apply',stash).decode('utf-8'))
    (HERE/'merge.log').write_text('\n'.join(log),encoding='utf-8')
    raise
after=files();assert set(after)==set(before['files'])
for name,value in after.items():
    assert value['status']==before['files'][name]['status'],name
    if name!=path:assert value==before['files'][name],name
actual=(TARGET/path).read_bytes().replace(b'\r\n',b'\n')
assert hashlib.sha256(actual).hexdigest()==before['expectedNormalizedSha256'],'Overlapping file differs from clean three-way preflight'
assert git('rev-parse','HEAD').decode().strip()==before['incoming']
assert not git('diff','--cached','--name-only').strip()
assert not git('ls-files','-i','-c','--exclude-standard').strip()
(HERE/'after.json').write_text(json.dumps({'head':before['incoming'],'files':after,'preserved':True,'unchanged_bytes':9,'combined_file':path,'stash':stash},indent=2),encoding='utf-8')
(HERE/'stash.json').write_text(json.dumps({'name':stash_name,'object':stash,'path':path,'restored':True,'retained_as_backup':True},indent=2),encoding='utf-8')
(HERE/'merge.log').write_text('\n'.join(log),encoding='utf-8')
print('MERGE_DONE',before['incoming'],'nine files byte-for-byte preserved; ProcessUnit combines both branches and original local edits; named stash restored and retained')

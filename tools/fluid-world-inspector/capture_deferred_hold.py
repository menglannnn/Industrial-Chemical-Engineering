"""Freeze current format-17 evidence read-only; no world load, process update or solver."""
from pathlib import Path
import datetime, hashlib, importlib.util, io, json, struct, subprocess, tarfile

ROOT=Path(__file__).resolve().parent
TARGET=Path(r'D:\Minecraft\Modding\1.21\CreateChemE\.claude\worktrees\rename-ichem-mod-30e50c')
WORLD=TARGET/'run/saves/New World'
spec=importlib.util.spec_from_file_location('nbt_reader',ROOT/'inspect_world.py')
nbt_reader=importlib.util.module_from_spec(spec);spec.loader.exec_module(nbt_reader)

def git(*args): return subprocess.check_output(['git',*args],cwd=TARGET).decode('utf-8').strip()
def add(tar,name,raw):
    info=tarfile.TarInfo(name);info.size=len(raw);tar.addfile(info,io.BytesIO(raw))

def main():
    data=WORLD/'data';core_path=data/'icheme_fluid_core.dat'
    for attempt in range(3):
        raw=core_path.read_bytes();core=nbt_reader.nbt(raw)['data']
        if core['FluidFormat']!=17: raise ValueError('Current checkpoint 17 required')
        try: packs={p.name:p.read_bytes() for p in (data/'icheme_fluid').glob('fluid-*.pack')}
        except FileNotFoundError: continue
        if core_path.read_bytes()==raw: break
    else: raise RuntimeError('Core changed during capture; retry when stable')
    rows=core['Islands'];units=[core['Topology']]
    for i in range(len(rows['Id'])):
        units.append({k:rows[k][i] for k in ['Pack','Offset','Length']}|{'SHA256':rows['SHA256'][32*i:32*i+32]})
    for ref in units:
        block=packs['fluid-%s.pack'%ref['Pack']][ref['Offset']:ref['Offset']+ref['Length']]
        if hashlib.sha256(block).digest()!=ref['SHA256']: raise ValueError('Copied unit SHA mismatch')
        if struct.unpack('>I',block[:4])[0]!=0x43434655 or block[4]!=10: raise ValueError('Current storage unit 10 required')
    logs=(TARGET/'run/logs/latest.log').read_bytes();text=logs.decode('utf-8',errors='replace')
    holds=[line for line in text.splitlines() if 'fluid_island=' in line and 'status=HELD' in line]
    now=datetime.datetime.now(datetime.timezone.utc)
    client_running=b'"32844"' in subprocess.check_output(['tasklist','/FI','PID eq 32844','/FO','CSV','/NH'])
    manifest={'captured_utc':now.isoformat(),'target':str(TARGET),'world':str(WORLD),'branch':git('branch','--show-current'),'head':git('rev-parse','HEAD'),'client_pid':32844,'client_left_running':client_running,'snapshot':'on-disk atomic core and immutable packs; no forced save','checkpoint_mtime_local':datetime.datetime.fromtimestamp(core_path.stat().st_mtime).isoformat(),'core':nbt_reader.compact(core),'unit_sha_validation':'PASS','held_log_lines':holds,'git_status':git('status','--short'),'files':{}}
    files={'world/data/icheme_fluid_core.dat':raw,**{'world/data/icheme_fluid/'+name:value for name,value in packs.items()},'logs/latest.log':logs,'logs/runClient.log':(TARGET/'build/pipeline-shutoff-target-client.log').read_bytes(),'source/target-local.patch':subprocess.check_output(['git','diff','--binary'],cwd=TARGET)}
    for rel in ['level.dat','level.dat_old']:
        if (WORLD/rel).is_file(): files['world/'+rel]=(WORLD/rel).read_bytes()
    for p in (TARGET/'run/config').glob('icheme*'):
        if p.is_file(): files['config/'+p.name]=p.read_bytes()
    for rel in git('ls-files','--others','--exclude-standard').splitlines():
        p=TARGET/rel
        if p.is_file(): files['source/untracked/'+rel]=p.read_bytes()
    manifest['files']={name:{'bytes':len(value),'sha256':hashlib.sha256(value).hexdigest()} for name,value in files.items()}
    output=ROOT/'logs'/('deferred-hold-logs-'+now.strftime('%Y-%m-%d-%H%M%S')+'.tar.gz');output.parent.mkdir(exist_ok=True)
    with tarfile.open(output,'w:gz') as tar:
        for name,value in files.items(): add(tar,name,value)
        add(tar,'manifest.json',json.dumps(manifest,indent=2).encode())
    (ROOT/'deferred-hold-capture.json').write_text(json.dumps({'archive':str(output),'captured_utc':manifest['captured_utc'],'checkpoint_mtime_local':manifest['checkpoint_mtime_local'],'epoch':core.get('Epoch'),'islands':{key:rows.get(key) for key in ['Id','Committed','Online']},'held_log_lines':holds,'unit_sha_validation':'PASS'},indent=2),encoding='utf-8')
    print(json.dumps({'archive':str(output),'compressed_bytes':output.stat().st_size,'checkpoint_mtime_local':manifest['checkpoint_mtime_local'],'epoch':core.get('Epoch'),'saved_islands':rows['Id'],'hold_lines':len(holds),'last_hold':holds[-1] if holds else None,'unit_sha_validation':'PASS'},indent=2))

if __name__=='__main__': main()

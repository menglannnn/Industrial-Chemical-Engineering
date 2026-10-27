"""Inspect only a frozen format-17/unit-10 archive, never a live world or a solver."""
from pathlib import Path
import hashlib, json, struct, sys, tarfile

sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'fluid-world-inspector'))
from inspect_world import Reader, nbt, compact, island, control

def topology(r):
    next_id=r.var();texts=[r.text() for _ in range(r.var())]
    geometries=[[r.f64() for _ in range(4)] for _ in range(r.var())]
    compositions=[r.sparse() for _ in range(r.var())]
    def position():return [texts[r.var()],r.i32(),r.i32(),r.i32()]
    def registration():
        d={'id':r.var(),'position':position(),'kind':['RESERVOIR','PIPE','PUMP','VALVE','GENERATOR','VOID','FILTER','COMPRESSOR'][r.u8()],'facing':r.u8()}
        d['geometry']=geometries[r.var()];d['control']=control(r);d['generatorEnabled']=r.flag()
        d['cutoff']=['NONE','OPEN','CLOSED'][r.u8()]
        if d['kind']!='PIPE' and d['cutoff']!='NONE':raise ValueError('Cutoff on non-pipe')
        d['ports']=[{'position':position(),'face':r.u8(),'phasePort':r.u8(),'role':r.u8()} for _ in range(r.var())]
        d['spec']={'volume':r.f64(),'temperature':r.f64(),'pressure':r.f64(),'composition':compositions[r.var()],'solidFraction':r.f64()}
        d['spec']['grades']=[{'material':texts[r.var()],'size':texts[r.var()],'massShare':r.f64()} for _ in range(r.var())]
        d['spec']['height']=r.f64();d['spec']['open']=r.flag();d['revision']=r.var()
        return d
    active=[registration() for _ in range(r.var())];pending=r.var()
    return {'nextIdentity':next_id,'active':active,'pendingCount':pending}

def inspect(source):
    with tarfile.open(source,'r:gz') as tar:
        def read(name):return tar.extractfile(name).read()
        core=nbt(read('world/data/icheme_fluid_core.dat'))['data']
        if core['FluidFormat']!=17:raise ValueError('Only checkpoint 17')
        def unit(ref):
            raw=read('world/data/icheme_fluid/fluid-%s.pack'%ref['Pack'])[ref['Offset']:ref['Offset']+ref['Length']]
            if hashlib.sha256(raw).digest()!=ref['SHA256']:raise ValueError('Unit hash mismatch')
            r=Reader(raw)
            if r.i32()!=0x43434655 or r.u8()!=10:raise ValueError('Only unit 10')
            return r,{'kind':r.u8(),'id':r.i64(),'revision':r.i64(),'generation':r.i64()}
        r,header=unit(core['Topology']);world=topology(r);rows=core['Islands'];islands=[]
        for i,identity in enumerate(rows['Id']):
            ref={k:rows[k][i] for k in ['Pack','Offset','Length']}|{'SHA256':rows['SHA256'][32*i:32*i+32]}
            r,header=unit(ref);entry=island(r,core['Strings']);entry['header']=header
            entry['clock']={key:rows[key][i] for key in ['Online','Committed','Retry','Cadence']};islands.append(entry)
        configs={m.name:read(m.name).decode() for m in tar.getmembers() if m.name.startswith('config/')}
        return {'core':compact(core),'world':world,'islands':islands,'configs':configs,'capture':json.loads(read('manifest.json'))}

if __name__=='__main__':
    output=inspect(Path(sys.argv[1]));Path(sys.argv[2]).write_text(json.dumps(output,indent=2),encoding='utf-8')
    print('epoch',output['core']['Epoch'],'pending',output['world']['pendingCount'])
    print('configs',output['configs'])
    held=next(e for e in output['islands'] if e['header']['id']==94)
    print('HELD',held['status'],'CLOCK',held['clock'])
    print('NODES',[(n['id'],n['kind'],n['elevation'],n['temperature'],n['pressure'],n['capacity']) for n in held['graph']['nodes']])
    print('PIPES',json.dumps(held['graph']['pipes'],indent=2))
    print('ENDPOINT',held.get('lastResult'))
    print('DEVICES',[(d['id'],d['kind'],d['position'],d['geometry'],d['control'],d['spec']['temperature'],d['spec']['pressure']) for d in output['world']['active']])

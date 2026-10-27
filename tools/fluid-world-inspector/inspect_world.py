"""Read-only format-16 fluid checkpoint inspection. Never starts Minecraft or a solver."""
from pathlib import Path
import gzip, hashlib, json, struct, sys

class Reader:
    def __init__(self, data): self.data, self.at = data, 0
    def raw(self, n):
        value = self.data[self.at:self.at+n]
        if len(value) != n: raise ValueError('Truncated data')
        self.at += n
        return value
    def number(self, fmt): return struct.unpack('>'+fmt, self.raw(struct.calcsize('>'+fmt)))[0]
    def u8(self): return self.number('B')
    def flag(self):
        value = self.u8()
        if value not in (0,1): raise ValueError('Malformed boolean')
        return bool(value)
    def i32(self): return self.number('i')
    def i64(self): return self.number('q')
    def f64(self): return self.number('d')
    def var(self):
        value = shift = 0
        while True:
            byte = self.u8(); value |= (byte & 127) << shift
            if not byte & 128: return value
            shift += 7
            if shift > 63: raise ValueError('Varint overflow')
    def text(self): return self.raw(self.var()).decode('utf-8')
    def sparse(self):
        n = self.var(); mask = self.raw((n+7)//8)
        return [self.f64() if mask[i//8] & (1 << (i%8)) else 0 for i in range(n)]

def nbt(data):
    r = Reader(gzip.decompress(data))
    def string(): return r.raw(r.number('H')).decode('utf-8')
    def payload(tag):
        if tag in (1,2,3,4,5,6): return r.number({1:'b',2:'h',3:'i',4:'q',5:'f',6:'d'}[tag])
        if tag == 7: return r.raw(r.i32())
        if tag == 8: return string()
        if tag == 9:
            kind, n = r.u8(), r.i32()
            return [payload(kind) for _ in range(n)]
        if tag == 10:
            value = {}
            while (kind := r.u8()):
                key = string(); value[key] = payload(kind)
            return value
        if tag in (11,12): return [r.number('i' if tag == 11 else 'q') for _ in range(r.i32())]
        raise ValueError('Unknown NBT type '+str(tag))
    kind = r.u8(); string()
    return payload(kind)

def compact(value):
    if isinstance(value,bytes): return {'bytes':len(value),'sha256':hashlib.sha256(value).hexdigest()}
    if isinstance(value,dict): return {k:compact(v) for k,v in value.items()}
    if isinstance(value,list): return [compact(v) for v in value]
    return value

def unit(core, data, ref):
    source = data / 'icheme_fluid' / ('fluid-%s.pack'%ref['Pack'])
    raw = source.read_bytes()[ref['Offset']:ref['Offset']+ref['Length']]
    if hashlib.sha256(raw).digest() != ref['SHA256']: raise ValueError('Unit hash mismatch')
    r = Reader(raw)
    if r.i32() != 0x43434655 or r.u8() != 9: raise ValueError('Only unit format 9 supported')
    header = {'kind':r.u8(),'id':r.i64(),'revision':r.i64(),'generation':r.i64()}
    return r, header

def control(r):
    kind = r.u8()
    if kind == 0: return {'kind':'passive'}
    if kind in (1,3): return {'kind':'pump' if kind == 1 else 'compressor','volumeFlow':r.f64(),'limit':r.f64(),'enabled':r.flag()}
    if kind == 2: return {'kind':'valve','pressure':r.f64()}
    raise ValueError('Unknown control')

def solids(r):
    return [{'material':r.text(),'revision':r.text(),'density':r.f64(),'heatCapacity':r.f64(),'size':r.text(),'mass':r.f64()} for _ in range(r.var())]

def graph(r):
    nodes = [{'id':r.i64(),'elevation':r.f64(),'kind':['RESERVOIR','JUNCTION','GENERATOR','VOID','PORT'][r.u8()],
              'capacity':r.f64(),'height':r.f64(),'open':r.flag()} for _ in range(r.var())]
    pipes = []
    for _ in range(r.var()):
        p = {'id':r.i64(),'first':r.var(),'second':r.var()}
        p['sections'] = [[r.f64() for _ in range(4)] for _ in range(r.var())]
        p['control'] = control(r); p['ports'] = [r.u8(),r.u8()]; pipes.append(p)
    for node in nodes:
        node.update(moles=r.sparse(),energy=r.f64(),solids=solids(r),temperature=r.f64(),pressure=r.f64(),liquid=r.sparse(),vapor=r.sparse(),waterLiquid=r.f64(),waterVapor=r.f64(),hydrocarbonPressure=r.f64())
    for p in pipes:
        p['blocked'] = r.var()
        p['filter'] = {'capacity':r.f64(),'solids':solids(r),'energy':r.f64(),'stopped':r.flag()} if r.flag() else None
    return {'nodes':nodes,'pipes':pipes}

def island(r, strings):
    result = {'dimension':strings[r.var()],'package':strings[r.var()],'status':r.text()}
    result['fences'] = [[r.i64(),r.i64(),r.i64()] for _ in range(r.var())]
    result['graph'] = graph(r)
    if r.flag():
        solved = {'seconds':r.f64(),'massFlows':r.sparse(),'accepted':r.var(),'rejected':r.var(),'pumpWork':r.f64()}
        solved['boundaries'] = [{'node':r.i64(),'moles':r.sparse(),'energy':r.f64(),'solids':solids(r),'solidDirection':r.u8()-1,'kind':r.u8()} for _ in range(r.var())]
        solved['reasons'] = [[r.text(),r.i32()] for _ in range(r.var())]
        solved['modes'] = [r.u8() for _ in range(r.var())]; solved['heads'] = r.sparse(); solved['endpointFlows'] = r.sparse()
        result['lastResult'] = solved
    return result

def topology(r):
    next_id = r.var(); texts = [r.text() for _ in range(r.var())]
    geometries = [[r.f64() for _ in range(4)] for _ in range(r.var())]
    compositions = [r.sparse() for _ in range(r.var())]
    def position(): return [texts[r.var()],r.i32(),r.i32(),r.i32()]
    def registration():
        result = {'id':r.var(),'position':position(),'kind':['RESERVOIR','PIPE','PUMP','VALVE','GENERATOR','VOID','FILTER','COMPRESSOR'][r.u8()],'facing':r.u8()}
        result['geometry'] = geometries[r.var()]; result['control'] = control(r)
        result['generatorEnabled'] = r.flag()
        result['ports'] = [{'position':position(),'face':r.u8(),'phasePort':r.u8(),'role':r.u8()} for _ in range(r.var())]
        result['spec'] = {'volume':r.f64(),'temperature':r.f64(),'pressure':r.f64(),'composition':compositions[r.var()],'solidFraction':r.f64()}
        result['spec']['grades'] = [{'material':texts[r.var()],'size':texts[r.var()],'massShare':r.f64()} for _ in range(r.var())]
        result['spec']['height'] = r.f64(); result['spec']['open'] = r.flag(); result['revision'] = r.var()
        return result
    active = [registration() for _ in range(r.var())]
    event_count = r.var()
    return {'nextIdentity':next_id,'texts':texts,'active':active,'pendingCount':event_count,'bytesRead':r.at}

def main():
    data = Path(sys.argv[1])
    core = nbt((data / 'icheme_fluid_core.dat').read_bytes())['data']
    if core['FluidFormat'] != 16: raise ValueError('Only checkpoint format 16 supported')
    result = {'core':compact(core)}
    r, header = unit(core,data,core['Topology'])
    result['topologyHeader'] = header; result['world'] = topology(r)
    rows = core['Islands']; islands = []
    for i, identity in enumerate(rows['Id']):
        ref = {key:rows[key][i] for key in ['Pack','Offset','Length']}; ref['SHA256'] = rows['SHA256'][32*i:32*i+32]
        r, header = unit(core,data,ref)
        entry = island(r,core['Strings']); entry['header'] = header
        entry['clock'] = {key:rows[key][i] for key in ['Online','Committed','Retry','Cadence']}
        islands.append(entry)
    result['islands'] = islands
    print(json.dumps(result,indent=2))

if __name__ == '__main__': main()

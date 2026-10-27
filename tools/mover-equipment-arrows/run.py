from pathlib import Path
import subprocess

ROOT=Path(__file__).resolve().parents[2]
TOOL=Path(__file__).resolve().parent
OUT=ROOT/'documentation/2026-10-24-mover-equipment-arrows/renders'
JAVA='C:/Program Files/Java/jdk-21.0.11/bin/java.exe'
CP=str(TOOL/'classes')+';build/classes/java/main'

def run(cls,args=(),baseline=False):
    cp=(str(TOOL/'baseline-classes')+';' if baseline else '')+CP
    return subprocess.check_output([JAVA,'-Xmx2g','-Djava.awt.headless=true','-cp',cp,'com.wormzjl.icheme.science.equipment.'+cls,*args],text=True)

before=run('PumpPorts',baseline=True)
after=run('PumpPorts')
(TOOL/'ports-before.txt').write_text(before,encoding='utf-8')
(TOOL/'ports-after.txt').write_text(after,encoding='utf-8')
assert before==after,'Connection fingerprint changed'
print(f'{len(after.splitlines())} pump size/heading/support cases: identical bounds, port faces, physical ports and hydraulic binding cells')

cases=[('pump','PUMP','.3','1'),('split-pump','PUMP','.75','1'),('reciprocating','COMPRESSOR','.45','2'),('centrifugal','CENTRIFUGAL_COMPRESSOR','1','2')]
with (TOOL/'renders.log').open('w',encoding='utf-8') as log:
    for stem,kind,size,count in cases:
        jobs=[(h,'0',True) for h in ['NORTH','EAST','SOUTH','WEST']]
        jobs += [('NORTH',od,True) for od in ['.0625','1']]
        jobs += [('NORTH','0',False)]
        for heading,od,attached in jobs:
            suffix='' if od=='0' and attached else ('-od'+od if attached else '-bare')
            prefix=str(OUT/(stem+'-'+heading.lower()+suffix))
            result=run('MoverPortRender',[kind,size,count,'auto',prefix,'iso' if od=='0' and attached else 'none',heading,od,str(attached).lower()])
            assert 'nozzle_marker_polygons=0' not in result and 'pipe_marker_polygons=0' in result
            log.write(result)
    log.flush()
print('28 render cases completed: connected/bare, four headings and drawn size extremes')

#!/usr/bin/env node
// condense.js <rig log>: one line per read of TestWorldRigTest output (row, time, tag, device, status, state, solids,
// outlets, closures), for comparing forms and the in-game reads: node index-lane/condense.js rig.log > rig.condensed.txt

const fs=require('fs');const lines=fs.readFileSync(process.argv[2],'utf8').split(/\r?\n/);
let cur=null;const out=[];
const flush=()=>{if(cur)out.push(cur.join(' | '));cur=null;};
for(const l of lines){
  let m=l.match(/^RIG (\S+) t=([\d.]+)s (\S+) (\S+) (RESERVOIR|PIPE|PUMP|VALVE|COMPRESSOR|GENERATOR) (?:island=\d+ )?(?:committed=[\d.]+s )?status=(.*?)(?: pipe \d+ .*)?$/);
  if(m){flush();let st=m[6].replace(/; reasons=\{.*$/,'').replace(/Substep refinement exhausted: /,'').slice(0,70);cur=[`${m[1]} ${m[2]}s ${m[3]} ${m[4]}`,st];
    const b=l.match(/blocked=\d+ q=([-\d.]+ kg\/s)( \/ .*)?$/);if(b)cur.push(`q=${b[1]}${b[2]?b[2]:''}`);continue;}
  if(!cur)continue;
  if((m=l.match(/T=([\d.]+) K .*P=([\d.]+) Pa .*\(fill ([\d.]+) %\) mass=([\d.]+) kg liquid=(\S+) water=\S+ vapour=(\S+) solids=(\S+) m3/)))cur.push(`T=${m[1]} P=${(m[2]/1000).toFixed(1)}kPa fill=${m[3]}% m=${m[4]}kg L=${(+m[5]*1000).toFixed(1)}L V=${(+m[6]*1000).toFixed(0)}L S=${(+m[7]*1000).toFixed(2)}L`);
  else if((m=l.match(/solid \S+ ([\d.]+) um ([\d.]+) kg/)))cur.push(`solid ${m[2]}kg@${m[1]}um`);
  else if((m=l.match(/outlet (\S+) (\S+) mass ([-\d.]+) kg\/s .*drawn=(-?\d+)/)))cur.push(`${m[1]}/${m[2]} ${m[3]}kg/s d${m[4]}`);
  else if((m=l.match(/edge \d+ .* (\w+) mode=(\S+) q=([-\d.]+) kg\/s .*blocked=(\d)( \/ .*)?$/))&&(m[4]!=='0'||m[2]==='INLET_WRONG_PHASE'||m[1]!=='Passive'))cur.push(`${m[1]}:${m[2]} q=${m[3]}${m[5]||''}`);
}
flush();console.log(out.join('\n'));

// Per-seed Newton iterations of two one-worker fuzz reports (perseed.sh): the seeds that account for the difference,
// with their holds and classes in both. Usage: node newton-per-seed.js <A-report> <B-report> [top]
const fs=require('fs');
function read(f){const m=new Map();let cur=null;for(const l of fs.readFileSync(f,'utf8').split(/\r?\n/)){
  let s=l.match(/^SEED (\d+) .*holds=(\d+) classes=\[([^\]]*)\]/);if(s){cur={seed:+s[1],holds:+s[2],cls:s[3]||'clean'};m.set(cur.seed,cur);continue;}
  let c=l.match(/COUNTERS_SEED newtonSolves=(\d+) newtonIterations=(\d+) equationGateRejections=(\d+)/);if(c&&cur){cur.solves=+c[1];cur.it=+c[2];cur.gate=+c[3];}}
  return m;}
const [a,b]=[read(process.argv[2]),read(process.argv[3])];const top=+(process.argv[4]||12);
let ta=0,tb=0,heldA=0,heldB=0,rows=[];
for(const [s,x] of a){const y=b.get(s);ta+=x.it;tb+=y.it;if(x.holds>0)heldA+=x.it;if(y.holds>0)heldB+=y.it;rows.push({s,x,y,d:x.it-y.it});}
console.log(`total A ${ta} B ${tb}; in seeds with a hold: A ${heldA} B ${heldB}; in clean seeds: A ${ta-heldA} B ${tb-heldB}`);
rows.sort((p,q)=>q.d-p.d);let cum=0;
for(const r of rows.slice(0,top)){cum+=r.d;console.log(`seed ${r.s}: A ${r.x.it} (holds ${r.x.holds}, ${r.x.cls}) B ${r.y.it} (holds ${r.y.holds}, ${r.y.cls}) diff ${r.d} cum ${cum}`);}
const med=x=>{const v=[...x].sort((p,q)=>p-q);return v[v.length>>1];};
console.log('median per seed A',med(rows.map(r=>r.x.it)),'B',med(rows.map(r=>r.y.it)));

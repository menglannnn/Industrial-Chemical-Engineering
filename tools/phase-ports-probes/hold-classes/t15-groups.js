// node groups.js : t13's E6-S / E6-B seeds, class per run (sf01 base, sf15 (a)+(b), sf16 (a)+(b)+(c))
const fs=require('fs');
const G={S:{defaults:[29,173,175,183,220,241,246,298,352,357,397,424,529,547,606,720,758,768,786,886,915,932,967,981],random:[19,92,98,107,118,151,244,600,633,660,794,842,886,916,937]},
B:{defaults:[228,336,475,563,600,651,824,842,951],random:[460,499,719]}};
const cls=(f)=>{const m=new Map();for(const l of fs.readFileSync(f,'utf8').split(/\r?\n/)){const c=l.split(/\s+/)[0];const k=l.indexOf('list=');if(!c||k<0)continue;for(const s of l.slice(k+5).trim().split(/\s+/))if(s)m.set(+s,c);}return m;};
for(const [g,ds] of Object.entries(G))for(const [d,seeds] of Object.entries(ds)){
  const runs=['sf01-base-alone','sf15-t13seeds-ab','sf16-t13seeds-abc'].map(p=>cls(`${p}-${d}-report.txt.classes.txt`));
  const tally=runs.map(m=>{const t={};for(const s of seeds){const c=m.get(s)||'clean';t[c]=(t[c]||0)+1;}return Object.entries(t).sort().map(([c,n])=>c+' '+n).join(', ');});
  console.log(`E6-${g} ${d} (${seeds.length}): base [${tally[0]}] | (a)+(b) [${tally[1]}] | +(c) [${tally[2]}]`);
  console.log('   per seed: '+seeds.map(s=>`${s}:${runs.map(m=>m.get(s)||'ok').join('/')}`).join(' '));
}

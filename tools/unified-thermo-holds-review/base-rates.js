// t37: hold rate of a HoldFuzzTest report by world feature (every seed, held or clean). Usage: node base-rates.js <report.txt>
const fs=require('fs');const L=fs.readFileSync(process.argv[2],'utf8').split(/\r?\n/);
const seeds=[];let cur=null;
for(const l of L){let m;if((m=l.match(/^SEED (\d+) blocks=(\d+).* holds=(\d+)/))){cur={seed:+m[1],blocks:+m[2],held:+m[3]>0};seeds.push(cur);}
 else if(l.startsWith('  layout ')&&cur){cur.layout=l;}}
const feat={
 'LNG gen':s=>/gen\[LNG/.test(s.layout),'CO2gas gen':s=>/gen\[CO2gas/.test(s.layout),'LNG+CO2gas':s=>/gen\[LNG/.test(s.layout)&&/gen\[CO2gas/.test(s.layout),
 'LNG only (no CO2gas)':s=>/gen\[LNG/.test(s.layout)&&!/gen\[CO2gas/.test(s.layout),'CO2gas only':s=>!/gen\[LNG/.test(s.layout)&&/gen\[CO2gas/.test(s.layout),
 'no gen':s=>!/gen\[/.test(s.layout),'water gen':s=>/gen\[water/.test(s.layout),'crude gen':s=>/gen\[crude/.test(s.layout),'CH4 gen':s=>/gen\[CH4/.test(s.layout),'N2 gen':s=>/gen\[N2/.test(s.layout),
 'any gen':s=>/gen\[/.test(s.layout),'tank':s=>/:tank/.test(s.layout),'void':s=>/:void/.test(s.layout),'pump':s=>/:pump/.test(s.layout),'comp':s=>/:comp/.test(s.layout),'valve':s=>/:valve/.test(s.layout),
 'LNG>=160K':s=>[...s.layout.matchAll(/gen\[LNG (\d+)K/g)].some(m=>+m[1]>=160),'LNG<125K':s=>[...s.layout.matchAll(/gen\[LNG (\d+)K/g)].some(m=>+m[1]<125),
 'blocks<=10':s=>s.blocks<=10,'blocks 11-20':s=>s.blocks>10&&s.blocks<=20,'blocks>20':s=>s.blocks>20};
for(const [k,f] of Object.entries(feat)){const a=seeds.filter(f);const h=a.filter(s=>s.held).length;console.log(k.padEnd(22),String(a.length).padStart(4),'seeds',String(h).padStart(3),'held',(a.length?(100*h/a.length).toFixed(1):'-')+'%');}

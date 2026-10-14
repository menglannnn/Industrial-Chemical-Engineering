// Pairs the printed lines of two G1 stdout files (gates.sh <prefix>-G1-stdout.txt) by their non-numeric skeleton, after the
// masking of printed-lines.sh, and reports the largest relative difference of any numeric token per pair: a histogram and
// the top pairs. Lines of one skeleton are paired in text order. Usage: node printed-maxrel.js <A-stdout> <B-stdout> [top]
// A fifth argument "lo,hi" lists only the pairs with lo < maxrel <= hi. Batch 2026-09-26-phase-ports-and-compressor, the close (C4).
const fs=require('fs');
const NUM=/[-+]?(?:\d+\.\d*|\.\d+|\d+)(?:[eE][-+]?\d+)?/g;
function lines(f){return fs.readFileSync(f,'utf8').split(/\r?\n/)
 .filter(l=>l&&!/^<\?xml |^\s*<testcase|^\s*<\/?testsuite|^\s*<properties|^\s*<\/properties|^\s*<property |^\]\]><\/system-|^\s*<system-/.test(l))
 .map(l=>l.replace(/(ms|wallMs|millis|allocatedMB|bytes|nanos|Nanos|wallNanos|elapsed)=[0-9.eE+-]+/g,'$1=*').replace(/time="[0-9.]+"/g,'time=*')
  .replace(/^\[[0-9:]+\] /,'').replace(/ [0-9.]+ ?ms\b/g,' * ms').replace(/ in [0-9.]+ ?s\b/g,' in * s')
  .replace(/reasons=\{([^}]*)\}/g,(m,g)=>'reasons={'+g.split(', ').sort().join(', ')+'}'));}
function group(ls){const m=new Map();for(const l of ls){const nums=(l.match(NUM)||[]).map(Number);const key=l.replace(NUM,'#');if(!m.has(key))m.set(key,[]);m.get(key).push({l,nums});}return m;}
const [A,B]=[process.argv[2],process.argv[3]].map(f=>group(lines(f)));const top=+(process.argv[4]||12);const band=process.argv[5]?process.argv[5].split(",").map(Number):null;
const pairs=[];let unpairedA=0,unpairedB=0;
for(const [k,as] of A){const bs=B.get(k)||[];const n=Math.min(as.length,bs.length);unpairedA+=as.length-n;unpairedB+=bs.length-n;
 as.sort((x,y)=>x.l<y.l?-1:1);bs.sort((x,y)=>x.l<y.l?-1:1);
 for(let i=0;i<n;i++){let mr=0,at=-1;for(let j=0;j<as[i].nums.length;j++){const a=as[i].nums[j],b=bs[i].nums[j];
  const d=a===b?0:Math.abs(a-b)/Math.max(Math.abs(a),Math.abs(b));if(d>mr){mr=d;at=j;}}pairs.push({mr,at,a:as[i].l,b:bs[i].l});}}
for(const [k,bs] of B)if(!A.has(k))unpairedB+=bs.length;
pairs.sort((x,y)=>y.mr-x.mr);
const bins=[[0,'identical'],[1e-14,'<=1e-14'],[1e-12,'<=1e-12'],[1e-9,'<=1e-9'],[1e-6,'<=1e-6'],[1e-3,'<=1e-3'],[Infinity,'>1e-3']];
const hist=bins.map(b=>[b[1],0]);for(const p of pairs){for(let i=0;i<bins.length;i++)if(p.mr<=bins[i][0]){hist[i][1]++;break;}}
console.log('pairs='+pairs.length+' unpairedA='+unpairedA+' unpairedB='+unpairedB);console.log(hist.map(h=>h[0]+'='+h[1]).join(' '));
const shown=band?pairs.filter(p=>p.mr>band[0]&&p.mr<=band[1]).slice(0,top):pairs.slice(0,top);
for(const p of shown)console.log('maxrel='+p.mr.toExponential(3)+' token#'+p.at+'\n  A: '+p.a.slice(0,220)+'\n  B: '+p.b.slice(0,220));

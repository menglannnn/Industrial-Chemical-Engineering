// Pairs the PHASE_PORT_PRIORITY_SLICE lines of two G1 stdout files by case and slice index (printed-maxrel.js pairs lines of one
// skeleton in sorted order, so a slice whose rejection-reason text differs is paired with another slice). Reports, per case, a
// histogram of the largest relative difference of the numeric tokens before accepted=, the number of slices whose accepted/rejected
// counts differ, and the worst pairs. Usage: node slice-pairs.js <A-stdout> <B-stdout> [top]. Batch 2026-09-26-phase-ports-and-compressor, the close (A423).
const fs=require('fs');const NUM=/[-+]?(?:\d+\.\d*|\.\d+|\d+)(?:[eE][-+]?\d+)?/g;
function load(f){const m=new Map();for(const l of fs.readFileSync(f,'utf8').split(/\r?\n/)){const h=l.match(/^(PHASE_PORT_PRIORITY_SLICE \S+ \d+) (.*)$/);if(!h)continue;
 const body=h[2].replace(/ accepted=.*$/,'');m.set(h[1],{body,tail:(h[2].match(/accepted=\d+ rejected=\d+/)||[''])[0]});}return m;}
const A=load(process.argv[2]),B=load(process.argv[3]);const hist={};let tails=0,n=0,worst=[];
for(const [k,a] of A){const b=B.get(k);if(!b)continue;n++;const na=a.body.match(NUM)||[],nb=b.body.match(NUM)||[];let r=0;
 for(let i=0;i<Math.min(na.length,nb.length);i++){const x=+na[i],y=+nb[i];if(x===y)continue;const d=Math.abs(x-y)/Math.max(Math.abs(x),Math.abs(y),1e-300);if(d>r)r=d;}
 if(a.tail!==b.tail)tails++;const c=k.split(' ')[1];const bin=r===0?'identical':r<=1e-14?'<=1e-14':r<=1e-12?'<=1e-12':r<=1e-9?'<=1e-9':r<=1e-6?'<=1e-6':r<=1e-3?'<=1e-3':'>1e-3';
 hist[c]=hist[c]||{};hist[c][bin]=(hist[c][bin]||0)+1;worst.push([r,k,a.tail,b.tail]);}
console.log('paired slices',n,'accepted/rejected tails differing',tails);for(const c of Object.keys(hist).sort())console.log(c,JSON.stringify(hist[c]));
worst.sort((p,q)=>q[0]-p[0]);for(const w of worst.slice(0,+process.argv[4]||6))console.log(w[0].toExponential(3),w[1],'|',w[2],'->',w[3]);

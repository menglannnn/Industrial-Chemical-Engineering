const fs=require('fs');
function write(file,o){
  const lines=['{'];
  lines.push(`  "id": ${JSON.stringify(o.id)},`);
  lines.push(`  "title": ${JSON.stringify(o.title)},`);
  lines.push('  "blocks": {');
  const ks=Object.keys(o.blocks);
  ks.forEach((k,i)=>lines.push(`    ${JSON.stringify(k)}: ${JSON.stringify(o.blocks[k]).replace(/,"/g,', "').replace(/":/g,'": ')}${i<ks.length-1?',':''}`));
  lines.push('  },');
  lines.push('  "steps": [');
  o.steps.forEach((s,i)=>lines.push(`    ${JSON.stringify(s).replace(/,"/g,', "').replace(/":/g,'": ')}${i<o.steps.length-1?',':''}`));
  lines.push('  ]');lines.push('}');
  fs.writeFileSync(file,lines.join('\n')+'\n');
}
const f1=JSON.parse(fs.readFileSync('rows-rig/D23-1-5cm.json','utf8'));
f1.id='F1-D23-1-5cm';f1.title='F1 bisect: D23 row 1 with the let-down pipe P2 left at 5 cm, traced at the first committed interval (TestWorldTrace.f1)';
f1.steps.splice(f1.steps.findIndex(s=>s.tag==='first-bucket')+1,0,{op:'trace',kind:'f1',id:'T'});
// (F1 row written earlier)
const f2=JSON.parse(fs.readFileSync('rows/02-R2-R7-R9.json','utf8'));
f2.id='F2-R9';f2.title='F2 bisect: E4 rows 2, 7 and 9 (R2-R7-R9), traced at the pump after the slow and the fast setting (TestWorldTrace.f2)';
let i=f2.steps.findIndex(s=>s.tag==='r9-slow');f2.steps.splice(i+1,0,{op:'trace',kind:'f2',id:'U'});
i=f2.steps.map(s=>s.tag).lastIndexOf('r9-fast');f2.steps.splice(i+1,0,{op:'trace',kind:'f2',id:'U'});
// (F2 row written earlier)
const m=JSON.parse(fs.readFileSync('rows-bisect/F2-R9.json','utf8'));
m.id='F2-R9-mint';m.title='F2 bisect: R2-R7-R9 traced at the pump right after it is placed, set and joined to its discharge (the pump junction mint)';
let k=m.steps.findIndex(s=>s.op==='place'&&s.ids.includes('U'));m.steps.splice(k+1,0,{op:'trace',kind:'f2',id:'U'});
k=m.steps.findIndex(s=>s.op==='set'&&s.id==='U');m.steps.splice(k+1,0,{op:'trace',kind:'f2',id:'U'});
k=m.steps.findIndex(s=>s.op==='place'&&s.ids.includes('Vp'));m.steps.splice(k+1,0,{op:'trace',kind:'f2',id:'U'});
const cut=m.steps.findIndex(s=>s.tag==='r9-slow');m.steps=m.steps.slice(0,cut+2);
write('rows-bisect/F2-R9-mint.json',m);

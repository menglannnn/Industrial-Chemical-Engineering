// t12 (2026-10-13): after each card's t11 paragraph in REMAINING_HOLDS.md, a paragraph restating the card on campaign q04
// (K with both rules), from logs/q15-card-lines.txt (card-lines.js q04 against o02 and p03). Run once from the worktree root.
const fs=require('fs');
const doc='documentation/2026-09-26-phase-ports-and-compressor/REMAINING_HOLDS.md';
const lines=fs.readFileSync('tools/phase-ports-probes/hold-classes/logs/q15-card-lines.txt','utf8').split(/\r?\n/);
const cards=new Map();let cls=null;
for(const l of lines){if(/^\S/.test(l)){cls=l.trim();cards.set(cls,{});continue;}const m=l.match(/^\s+(defaults|random) (\d+): (.*)$/);if(m&&cls){const [list,rest]=m[3].split('; against o02 ');const [o02,p03]=rest.split('; against p03 ');cards.get(cls)[m[1]]={n:m[2],list,o02,p03};}}
const text=fs.readFileSync(doc,'utf8');const out=[];let card=null;
for(const l of text.split('\n')){
  out.push(l);const h=l.match(/^## (\w+):/);if(h)card=h[1];
  if(l.startsWith('**On the side branch `claude/keep-contents` (t11')&&cards.has(card)){
    const c=cards.get(card),d=c.defaults,r=c.random;
    out.push('');
    out.push(`**On the side branch \`claude/keep-contents\` (t12: option K with both rules, an unchanged island kept and a displaced junction started as what arrives; \`67a329f8\`, campaign \`q04\`): defaults ${d.n} (${d.list}), random ${r.n} (${r.list}).** Against \`o02\`: defaults ${d.o02}; random ${r.o02}. Against \`p03\` (K): defaults ${d.p03}; random ${r.p03}.`);
  }
}
fs.writeFileSync(doc,out.join('\n'));

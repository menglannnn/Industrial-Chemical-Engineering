// Compares the per-class printed output of two lane runs: node cmp.js <a> <b> [class filter]
const fs = require('fs');
function sections(file) {
  const out = new Map(); let cur = null;
  for (const line of fs.readFileSync(file, 'utf8').replace(/\r/g, '').split('\n')) {
    const m = line.match(/^### (\S+)( tests=.*)?$/);
    if (m) { if (m[2]) { cur = null; } else { cur = m[1]; out.set(cur, []); } continue; }
    if (cur) out.get(cur).push(line);
  }
  return out;
}
const a = sections(process.argv[2]), b = sections(process.argv[3]);
for (const [cls, linesB] of b) {
  if (!a.has(cls)) { console.log('ONLY IN B: ' + cls); continue; }
  const linesA = a.get(cls);
  const setA = new Set(linesA), setB = new Set(linesB);
  const onlyA = linesA.filter(l => !setB.has(l)), onlyB = linesB.filter(l => !setA.has(l));
  console.log(`== ${cls}: ${linesA.length} vs ${linesB.length} lines, ${onlyA.length} only in A, ${onlyB.length} only in B`);
  if (process.argv[4] && !cls.includes(process.argv[4])) continue;
  for (const l of onlyA) console.log('  A: ' + l.slice(0, 400));
  for (const l of onlyB) console.log('  B: ' + l.slice(0, 400));
}

// Per-seed class transitions between two classified campaign reports (<report>.classes.txt from classify.sh).
// Usage: node transitions.js <A.classes.txt> <B.classes.txt>
// Prints the cleared seeds grouped by their class in A, the worse seeds grouped by their class in B, and the class
// counts of both. A seed absent from every class list is clean.
const fs = require('fs');
const read = f => {
  const m = new Map();
  for (const line of fs.readFileSync(f, 'utf8').split(/\r?\n/)) {
    const cls = line.split(/\s+/)[0]; const k = line.indexOf('list=');
    if (!cls || k < 0) continue;
    for (const s of line.slice(k + 5).trim().split(/\s+/)) if (s) m.set(Number(s), cls);
  }
  return m;
};
const [a, b] = [read(process.argv[2]), read(process.argv[3])];
const group = (seeds, by) => { const g = {}; for (const s of seeds) (g[by.get(s)] ??= []).push(s); return Object.entries(g).sort((x, y) => y[1].length - x[1].length).map(([c, l]) => `${c} ${l.length} (${l.sort((p, q) => p - q).join(' ')})`).join('; '); };
const cleared = [...a.keys()].filter(s => !b.has(s)), worse = [...b.keys()].filter(s => !a.has(s));
const moved = [...a.keys()].filter(s => b.has(s) && a.get(s) !== b.get(s)).sort((p, q) => p - q);
console.log(`cleared ${cleared.length}: ${group(cleared, a)}`);
console.log(`worse ${worse.length}: ${group(worse, b)}`);
console.log(`moved class ${moved.length}: ${moved.map(s => `${s} ${a.get(s)}->${b.get(s)}`).join(', ')}`);
const count = m => { const c = {}; for (const v of m.values()) c[v] = (c[v] ?? 0) + 1; return c; };
const ca = count(a), cb = count(b), all = [...new Set([...Object.keys(ca), ...Object.keys(cb)])].sort();
console.log('classes A -> B: ' + all.map(k => `${k} ${ca[k] ?? 0}->${cb[k] ?? 0}`).join(', '));

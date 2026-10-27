// One line per hold class for REMAINING_HOLDS.md: the class's seeds in campaign C (both distributions), and against two
// earlier campaigns A and B the seeds that left the class (cleared = clean in C, moved = another class in C) and arrived.
// Usage: node card-lines.js <prefix C> <prefix A> <tag A> <prefix B> <tag B>   (prefix = logs/...-campaign-x, without -random-report...)
const fs = require('fs');
const read = f => { const m = new Map(); for (const line of fs.readFileSync(f, 'utf8').split(/\r?\n/)) { const cls = line.split(/\s+/)[0]; const k = line.indexOf('list='); if (!cls || k < 0) continue; for (const s of line.slice(k + 5).trim().split(/\s+/)) if (s) m.set(Number(s), cls); } return m; };
const [c, a, ta, b, tb] = process.argv.slice(2);
const load = p => ({ random: read(`${p}-random-report.txt.classes.txt`), defaults: read(`${p}-defaults-report.txt.classes.txt`) });
const C = load(c), A = load(a), B = load(b);
const classes = new Set(); for (const x of [C, A, B]) for (const d of ['random', 'defaults']) for (const v of x[d].values()) classes.add(v);
const of = (m, k) => [...m.entries()].filter(([, v]) => v === k).map(([s]) => s).sort((p, q) => p - q);
for (const k of [...classes].sort()) {
  const parts = [];
  for (const d of ['defaults', 'random']) {
    const now = of(C[d], k);
    const diff = (old, tag) => {
      const before = of(old[d], k), cleared = before.filter(s => !C[d].has(s)), moved = before.filter(s => C[d].has(s) && C[d].get(s) !== k), arrived = now.filter(s => old[d].get(s) !== k);
      return `against ${tag} ${before.length} -> ${now.length} (cleared ${cleared.length}${cleared.length ? ': ' + cleared.join(' ') : ''}; moved out ${moved.length}${moved.length ? ': ' + moved.map(s => s + '>' + C[d].get(s)).join(' ') : ''}; new ${arrived.length}${arrived.length ? ': ' + arrived.map(s => s + (old[d].has(s) ? '<' + old[d].get(s) : '<clean')).join(' ') : ''})`;
    };
    parts.push(`${d} ${now.length}: ${now.join(' ') || 'none'}; ${diff(A, ta)}; ${diff(B, tb)}`);
  }
  console.log(`${k}\n  ${parts.join('\n  ')}`);
}

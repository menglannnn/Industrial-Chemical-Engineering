// The first-interval family (A97): held seeds whose first hold is on the first interval of an island formed by a
// placement, i.e. the first HELD line's committed tick equals a placement tick of the seed's layout (a replaced island
// starts committed at its edit's tick). Classes from <report>.classes.txt (classify.sh), first hold from the report.
// Usage: node first-interval.js <A-report> [<B-report>]   (prints each report's family by class; with two, the
// family's seeds cleared, kept and new between them)
const fs = require('fs');
const read = f => {
  const seeds = new Map(); let seed = null;
  for (const line of fs.readFileSync(f, 'utf8').split(/\r?\n/)) {
    let m;
    if ((m = line.match(/^SEED (\d+) .*holds=(\d+)/))) { seed = { id: +m[1], holds: +m[2], ticks: new Set(), first: null }; seeds.set(seed.id, seed); continue; }
    if (!seed) continue;
    if ((m = line.match(/^ {2}layout (.*)$/))) { for (const b of m[1].trim().split(/\s+/)) { const t = b.split(':')[0]; if (/^\d+$/.test(t)) seed.ticks.add(+t); } continue; }
    if (!seed.first && (m = line.match(/^ {2}HELD tick=(\d+) island=(\d+) committed=(\d+)/))) seed.first = { tick: +m[1], committed: +m[3] };
  }
  const classes = new Map();
  if (fs.existsSync(f + '.classes.txt')) for (const line of fs.readFileSync(f + '.classes.txt', 'utf8').split(/\r?\n/)) {
    const cls = line.split(/\s+/)[0]; const k = line.indexOf('list='); if (!cls || k < 0) continue;
    for (const s of line.slice(k + 5).trim().split(/\s+/)) if (s) classes.set(+s, cls);
  }
  const family = new Map();
  for (const s of seeds.values()) if (s.holds > 0 && s.first && s.ticks.has(s.first.committed)) family.set(s.id, classes.get(s.id) ?? '?');
  return { seeds, classes, family };
};
const show = (name, r) => {
  const by = {}; for (const [s, c] of r.family) (by[c] ??= []).push(s);
  const held = [...r.seeds.values()].filter(s => s.holds > 0).length;
  console.log(`${name}: held ${held}, first interval after a placement ${r.family.size}: ` +
    Object.entries(by).sort((a, b) => b[1].length - a[1].length).map(([c, l]) => `${c} ${l.length} (${l.sort((p, q) => p - q).join(' ')})`).join('; '));
};
const a = read(process.argv[2]); show(process.argv[2], a);
if (process.argv[3]) {
  const b = read(process.argv[3]); show(process.argv[3], b);
  const fmt = (l, m) => l.sort((p, q) => p - q).map(s => `${s}${m ? ':' + m(s) : ''}`).join(' ');
  const cleared = [...a.family.keys()].filter(s => !b.family.has(s));
  const fate = s => b.classes.get(s) ? `${b.classes.get(s)}-later` : 'clean';
  console.log(`family A -> B: kept ${[...a.family.keys()].filter(s => b.family.has(s)).length}; left ${cleared.length} (${fmt(cleared, fate)}); new ${[...b.family.keys()].filter(s => !a.family.has(s)).length} (${fmt([...b.family.keys()].filter(s => !a.family.has(s)), s => b.family.get(s))})`);
}

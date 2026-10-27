// M3a: compares probe outputs case by case: status/classification, T, P, crystal, TP equilibria and kernel calls, and
// totals of TP equilibria and kernel calls per mode prefix. Usage: node compare.js <a> <b> [<c> ...]
const fs = require('fs');
const files = process.argv.slice(2);
const parse = f => {
  const m = new Map();
  for (const l of fs.readFileSync(f, 'utf8').replace(/\r/g, '').split('\n')) {
    const i = l.indexOf(': ');
    if (i < 0 || l.startsWith('IUV') || l.startsWith('instrumented')) continue;
    const key = l.slice(0, i), rest = l.slice(i + 2);
    const g = (re) => { const x = rest.match(re); return x ? x[1] : null; };
    m.set(key, {status: rest.split(' ').slice(0, 2).join(' '), T: g(/ T (\S+) K/), P: g(/ P (\S+) Pa/), crystal: g(/ crystal (\S+) mol/),
      tp: +g(/ TPeq (\d+)/), kernel: +g(/ kernel (\d+)/), rule: rest.includes('| rule'), raw: rest});
  }
  return m;
};
const runs = files.map(parse);
const keys = [...runs[0].keys()];
const totals = runs.map(() => new Map());
for (const k of keys) {
  const rows = runs.map(r => r.get(k));
  const prefix = k.split(' ')[0];
  rows.forEach((r, i) => { if (!r) return; const t = totals[i].get(prefix) || {tp: 0, kernel: 0, conv: 0, n: 0}; t.tp += r.tp; t.kernel += r.kernel; t.n++; if (r.status.startsWith('CONVERGED')) t.conv++; totals[i].set(prefix, t); });
  const same = rows.every(r => r && r.status === rows[0].status && r.T === rows[0].T && r.P === rows[0].P && r.crystal === rows[0].crystal);
  const counts = rows.map(r => r ? `${r.tp}/${r.kernel}` : '-').join(' | ');
  const states = same ? 'same' : rows.map(r => r ? `${r.status} T ${r.T} P ${r.P} c ${r.crystal}` : '-').join(' || ');
  console.log(`${k}: ${counts}  ${states}`);
}
for (const [prefix] of totals[0]) console.log(`TOTAL ${prefix}: ` + totals.map(t => { const x = t.get(prefix); return `${x.conv}/${x.n} conv, TPeq ${x.tp}, kernel ${x.kernel}`; }).join(' | '));

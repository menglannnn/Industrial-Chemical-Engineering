// t20 (physical start, 2026-10-13): mechanism groups of traced first-interval holds (t20-trace.sh output), t13's group rule
// (t13-classify.js, A164) preceded by the mechanism the physical start adds:
//   L  a junction minted gas-only receives a liquid stream: the worst row of most failed passes of the hold tick is that
//      junction's mixing, energy or equilibrium row (enthalpy, frac, wsat, lnfug, pclose, continuity), its start holds no
//      liquid, and the island holds a liquid source (a generator or tank with liquid); the pass is laid out with the seed's
//      phases (gas), so the arriving liquid has no phase, its enthalpy drives the temperature out of the data (refused
//      trials: water below 273.16 K, nitrogen above 900 K, crude below 293.15 K) and the pass never converges.
//      Also L: a liquid source and a refused trial temperature (or a domain hold on a temperature) with a gas-only start.
//   C0 a gas-only island whose junctions were minted below their neighbours: every connection into them on its velocity
//      cap, nothing leaving, so their continuity rows do not depend on any pressure (singular, or a stalled walk): the
//      flat capped row of card E6-C at the first instant. Read: no liquid source and the worst row of at least half the
//      failed passes a gas-start junction's row.
//   then t13's rule on its classification line: T, B, C, S.
// Usage: node t20-groups.js <trace-dir> [--seeds]   (prints a table per group; --seeds lists the seeds per group and dist)
const fs = require('fs'), path = require('path');
const dir = process.argv[2]; const listSeeds = process.argv.includes('--seeds');
const t13 = new Map();
const cls = path.join(path.dirname(dir), path.basename(dir) + '-classification.txt');
for (const line of fs.readFileSync(cls, 'utf8').split(/\r?\n/)) {
  const f = line.split(' | ').map(s => s.trim()); if (f.length < 3 || !/^[SBCT]$/.test(f[0])) continue;
  t13.set(f[1][0] + '-' + f[2], f[0]);
}
const nodeRow = /^node (\d+) (enthalpy|frac:\S+|wsat|lnfug:\S+|pclose|continuity|energy)$/;
const rows = [];
for (const file of fs.readdirSync(dir).filter(f => /^[dr]-\d+\.txt$/.test(f))) {
  const L = fs.readFileSync(path.join(dir, file), 'utf8').split(/\r?\n/);
  const key = file.replace('.txt', ''); const dist = key[0] === 'd' ? 'defaults' : 'random', seed = +key.slice(2);
  const held = (L.find(l => l.startsWith('T13_HELD')) || '').replace(/^T13_HELD Held\[tick=\d+, island=\d+, committed=\d+, status=/, '');
  if (!held || held === 'null') { rows.push({ dist, seed, group: 'clean', kind: 'clean', text: '' }); continue; }
  const nodes = new Map(); let liquidSource = false;
  for (const l of L) {
    const m = l.match(/^ {4}node (\d+) id=\S+ (\w+) .* liq=(\S+) water=(\S+) vap=(\S+)/); if (!m) continue;
    nodes.set(+m[1], { kind: m[2], liq: +m[3], water: +m[4] });
    if (m[2] !== 'JUNCTION' && m[2] !== 'VOID' && (+m[3] > 0 || +m[4] > 0)) liquidSource = true;
  }
  let passes = 0, gasJunction = 0;
  for (const l of L.filter(l => l.startsWith('T13_PASS'))) {
    const r = l.match(/rows: \[([^\]]*?) r=/); if (!r) continue; passes++;
    const n = r[1].match(nodeRow); if (!n) continue;
    const node = nodes.get(+n[1]); if (node && node.kind === 'JUNCTION' && node.liq === 0 && node.water === 0) gasJunction++;
  }
  const kind = /thermo domain/.test(held) ? 'domain' : /Singular/.test(held) ? 'singular' : /active-set cycle/.test(held) ? 'cycle'
    : /iteration limit/.test(held) ? 'limit' : /line search/.test(held) ? 'stall' : /deadline|budget/.test(held) ? 'budget' : 'other';
  const g = t13.get(key[0] + '-' + seed) || '?';
  const share = passes ? gasJunction / passes : 0, refusedT = /thermo-domain: \w+ temperature/.test(held) || /temperature .* (below|above)|K is (below|above)/.test(held);
  const group = liquidSource && (share >= .25 || refusedT) ? 'L' : !liquidSource && share >= .5 ? 'C0' : g;
  const text = held.replace(/[-+]?\d+(\.\d+)?(E[-+]?\d+)?/g, '#').slice(0, 90);
  rows.push({ dist, seed, group, t13: g, kind, share: passes ? (gasJunction / passes).toFixed(2) : '-', text });
}
const by = {};
for (const r of rows) (by[r.group] ??= []).push(r);
for (const [g, list] of Object.entries(by).sort((a, b) => b[1].length - a[1].length)) {
  const d = list.filter(r => r.dist === 'defaults'), r = list.filter(r => r.dist === 'random');
  const kinds = {}; for (const x of list) kinds[x.kind] = (kinds[x.kind] || 0) + 1;
  console.log(`${g}: defaults ${d.length}, random ${r.length}; hold kinds ${Object.entries(kinds).map(([k, v]) => k + ' ' + v).join(', ')}`);
  if (listSeeds) for (const [n, s] of [['defaults', d], ['random', r]]) if (s.length) console.log(`  ${n}: ${s.map(x => x.seed).sort((a, b) => a - b).join(' ')}`);
}
if (process.argv.includes('--rows')) for (const r of rows.sort((a, b) => (a.dist + a.seed).localeCompare(b.dist + b.seed))) console.log([r.group, r.t13, r.dist, r.seed, r.kind, r.share, r.text].join(' | '));

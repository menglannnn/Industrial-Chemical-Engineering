// t37 (2026-10-18): one row per held seed of a HoldFuzzTest report: the first hold (as classify.sh reads it), the topology
// event that preceded it and the held island's world, rebuilt from the layout by the harness's own adjacency rule
// (HoldFuzzTest.connects; only pipes join, decision D20). Usage: node holds-table.js <dist> <report.txt> [classes.txt]
const fs = require('fs');
const [dist, reportPath, classesPath] = process.argv.slice(2);
const text = fs.readFileSync(reportPath, 'utf8').split(/\r?\n/);
const classOf = {};
if (classesPath && fs.existsSync(classesPath))
  for (const l of fs.readFileSync(classesPath, 'utf8').split(/\r?\n/)) {
    const m = l.match(/^(\S+)\s.*list=\s*(.*)$/); if (!m) continue;
    for (const s of m[2].trim().split(/\s+/)) classOf[s] = m[1];
  }
const STEP = { DOWN: [0, -1, 0], UP: [0, 1, 0], NORTH: [0, 0, -1], SOUTH: [0, 0, 1], WEST: [-1, 0, 0], EAST: [1, 0, 0] };
const AXIAL = new Set(['pump', 'comp', 'valve', 'filter']);
function parseBlock(tok) {
  const m = tok.match(/^(\d+):([a-z]+)([^@]*)@(-?\d+),(-?\d+),(-?\d+)(?:>(\w+))?$/); if (!m) return null;
  let kind = m[2], par = m[3];
  if (kind === 'pipe') { par = kind + par; }
  return { tick: +m[1], kind, par, label: m[2] + m[3], x: +m[4], y: +m[5], z: +m[6], facing: m[7] || null };
}
function connects(b, d) { if (!AXIAL.has(b.kind)) return true; const f = STEP[b.facing], s = STEP[d]; return f[0] * s[0] + f[1] * s[1] + f[2] * s[2] !== 0; }
function opposite(d) { return { DOWN: 'UP', UP: 'DOWN', NORTH: 'SOUTH', SOUTH: 'NORTH', WEST: 'EAST', EAST: 'WEST' }[d]; }
function components(blocks) {
  const at = new Map(blocks.map(b => [b.x + ',' + b.y + ',' + b.z, b]));
  for (const b of blocks) { b.links = []; }
  for (const b of blocks) for (const d of Object.keys(STEP)) {
    const s = STEP[d], n = at.get((b.x + s[0]) + ',' + (b.y + s[1]) + ',' + (b.z + s[2]));
    if (!n || !connects(b, d) || !connects(n, opposite(d))) continue;
    if (b.kind !== 'pipe' && n.kind !== 'pipe') continue;
    b.links.push(n);
  }
  const comp = new Map(); let k = 0;
  for (const b of blocks) { if (comp.has(b)) continue; const q = [b]; comp.set(b, k); while (q.length) { const c = q.pop(); for (const n of c.links) if (!comp.has(n)) { comp.set(n, k); q.push(n); } } k++; }
  return comp;
}
function digest(reasons) {
  const out = []; const add = (re, tag) => { let n = 0; for (const m of reasons.matchAll(re)) n += +m[1]; if (n) out.push(tag + n); };
  add(/active-set cycle=(\d+)/g, 'cyc'); add(/line search stalled at residual #(?:; active-set pass=#)?=(\d+)/g, 'ls');
  add(/iteration limit at residual #(?:; active-set pass=#)?=(\d+)/g, 'lim'); add(/state change #=(\d+)/g, 'BEchg');
  add(/boundary reopened=(\d+)/g, 'reopen'); add(/must be finite=(\d+)/g, 'nonfinite'); add(/Singular Newton Jacobian[^=]*=(\d+)/g, 'sing');
  add(/No finite Jacobian[^=]*=(\d+)/g, 'noFiniteJac'); add(/Solid transport transition[^=]*=(\d+)/g, 'solidTr'); add(/TP equilibrium did not converge[^=]*=(\d+)/g, 'tpFail');
  add(/equation gate: #=(\d+)/g, 'gate'); add(/Solid inventory overflow\/underflow=(\d+)/g, 'solidOvf');
  return out.join(',');
}
const rows = [];
let seed = null, blocksN = 0, layout = null, first = null;
function flush() {
  if (seed == null || !first) return;
  const s = first.status;
  const primary = s.replace(/^HELD: (Substep refinement exhausted: )?/, '').replace(/[;,] reasons=.*/, '').replace(/at residual [-0-9.E]+/, 'at |r|').replace(/; active-set pass=\d+/, '').replace(/advanced=.*/, '').slice(0, 60);
  const resid = (s.match(/at residual ([-0-9.E]+)/) || [])[1] || (s.match(/last=.*?([0-9.]+E-?\d+|[0-9]+\.[0-9]+)$/) || [])[1] || '';
  const pass = (s.match(/active-set pass=(\d+)/) || [])[1] || '';
  const reasons = (s.match(/reasons=\{(.*)\}/) || [])[1] || '';
  const placed = layout.filter(b => b.tick <= first.tick);
  const comp = components(placed);
  const trig = placed.filter(b => b.tick >= first.committed - 2 && b.tick <= first.tick);
  const event = first.tick - first.committed <= 20;
  let target;
  if (trig.length) { const counts = {}; for (const b of trig) counts[comp.get(b)] = (counts[comp.get(b)] || 0) + (b.kind === 'pipe' ? 1 : 2); target = +Object.entries(counts).sort((a, b) => b[1] - a[1])[0][0]; }
  else { const before = placed.filter(b => b.tick <= first.committed).sort((a, b) => b.tick - a.tick); target = before.length ? comp.get(before[0]) : 0; }
  const isl = placed.filter(b => comp.get(b) === target);
  const pick = k => isl.filter(b => b.kind === k);
  const gens = pick('gen').map(b => b.par.replace(/[\[\]]/g, '').replace(/ /g, '_') + '@y' + b.y);
  const pipes = pick('pipe'); const junctions = pipes.filter(b => b.links.length >= 3).length;
  const bores = [...new Set(pipes.map(b => b.par.replace('pipe', '')))].sort().join('/');
  const ys = isl.map(b => b.y);
  const devs = ['tank', 'void', 'pump', 'comp', 'valve', 'filter'].map(k => { const l = pick(k); return l.length ? k + (k === 'pump' || k === 'comp' || k === 'valve' ? l.map(b => b.par).join('') : 'x' + l.length) : ''; }).filter(Boolean).join(' ');
  rows.push([dist, seed, classOf[seed] || '?', blocksN, first.tick, first.committed, first.island, event ? 'event' : 'interval',
    trig.map(b => b.label).join(' ') || '-', primary, resid, pass, digest(reasons), gens.join(' ') || '-', devs || '-',
    pipes.length + 'p ' + junctions + 'j ' + bores, (Math.max(...ys) - Math.min(...ys)) + 'm', isl.length + '/' + placed.length].join('\t'));
}
for (const line of text) {
  let m;
  if ((m = line.match(/^SEED (\d+) blocks=(\d+)/))) { flush(); seed = m[1]; blocksN = +m[2]; layout = []; first = null; continue; }
  if (line.startsWith('  layout ')) { layout = [...line.slice(9).matchAll(/(\d+):([a-z]+(?:\[[^\]]*\]|[0-9.]*))@(-?\d+),(-?\d+),(-?\d+)(?:>(\w+))?/g)].map(m => parseBlock(m[0])).filter(Boolean); continue; }
  if (!first && (m = line.match(/^  HELD tick=(\d+) island=(\d+) committed=(\d+) class=\S+ status=(.*)$/))) first = { tick: +m[1], island: m[2], committed: +m[3], status: m[4] };
}
flush();
console.log(['dist', 'seed', 'class', 'blocks', 'heldTick', 'committed', 'island', 'trigger', 'eventBlocks', 'primary', 'resid', 'pass', 'rejections', 'islandFeeds', 'islandDevices', 'islandPipes', 'islandHeight', 'islandBlocks/placed'].join('\t'));
for (const r of rows) console.log(r);

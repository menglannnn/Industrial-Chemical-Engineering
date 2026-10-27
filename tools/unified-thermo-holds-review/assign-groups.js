// t37 (2026-10-18): the mechanism group of each held seed and the node or line at its hold, into holds-table.tsv.
// Groups K1-K8 (NON_CONVERGED_CASES_REVIEW.md). Evidence: "trace" (a t37 row trace), "prior" (an earlier trace of the same
// seed), "signature" (the hold's status and the held island's state, by the rules below). Usage:
//   node assign-groups.js raw-cold.tsv raw-bundled.tsv islands-cold.txt islands-bundled.txt > holds-table.tsv
const fs = require('fs');
const [rawC, rawB, islC, islB] = process.argv.slice(2);
const GROUP = {
  K1: 'choked let-down or vent lines: velocity-cap (and mover-mode) active set',
  K2: 'CO2-rich gas mixed into LNG near the CH4-CO2 critical region: check and Newton disagree',
  K3: 'a phase at a node\'s resolution: trace arrival, bubble-point receiver, 1e-8 gate',
  K4: 'a tank crossing to liquid-full: taut bag on stiff liquid',
  K5: 'denormal solid dust in a flowing junction',
  K6: 'a pump on a boiling or drained suction',
  K7: 'hot crude with free water: engine trial failures (bundled)',
  K8: 'budget: a costly but sensible world (wall deadline)',
};
const TRACED = { 'cold/694': 'K1', 'cold/535': 'K1', 'cold/518': 'K2', 'cold/1': 'K3', 'cold/521': 'K5', 'bundled/504': 'K4' };
const PRIOR = { 'cold/19': ['K1', 'M7'], 'cold/430': ['K1', 'M8, D39'], 'cold/48': ['K3', 'M8, D39'], 'bundled/47': ['K8', 'M5'],
  'bundled/431': ['K3', 'A418 first grams'], 'bundled/446': ['K3', 'A412 bubble point'], 'bundled/803': ['K3', 'A416 junction traces'],
  'bundled/157': ['K7', 'D44'], 'bundled/579': ['K7', 'D44'], 'bundled/698': ['K7', 'D44'], 'bundled/372': ['K1', 'A341 capped junctions'], 'bundled/600': ['K1', 'gas compressor world'] };
const SIGNATURE = { 'cold/181': 'K6', 'cold/397': 'K6', 'cold/757': 'K6', 'bundled/752': 'K6', 'bundled/779': 'K4', 'bundled/241': 'K8', 'bundled/430': 'K8', 'cold/589': 'K1' };
function islands(path) {
  const out = {}; let cur = null;
  for (const l of fs.readFileSync(path, 'utf8').split(/\r?\n/)) {
    let m;
    if ((m = l.match(/^SEED (\d+)/))) { cur = out[m[1]] = { nodes: [], edges: [] }; continue; }
    if (!cur) continue;
    if ((m = l.match(/^  n(\d+) (\w+) z=(\S+) (\S+)kPa (\S+)K m=(\S+) (\S+) liq=(\S+) vap=(\S+)/)))
      cur.nodes.push({ i: +m[1], kind: m[2], z: +m[3], P: +m[4], T: +m[5], m: +m[6], ph: m[7], liq: +m[8], vap: +m[9] });
    else if ((m = l.match(/^  e(\d+) n(\d+)->n(\d+) (\S+).*D=(\d+)mm/))) cur.edges.push({ i: +m[1], a: +m[2], b: +m[3], ctl: m[4], D: +m[5] });
    else if (l.startsWith('  last modes=')) cur.modes = l.match(/modes=\[([^\]]*)\]/)[1].split(', ');
  }
  return out;
}
const ISL = { cold: islands(islC), bundled: islands(islB) };
const empty = x => x.kind === 'RESE' && Math.abs(x.T - 298.1) < 0.2 && Math.abs(x.P - 101) < 1;
const fmt = x => `n${x.i} ${x.kind === 'JUNC' ? 'junction' : x.kind === 'RESE' ? (empty(x) ? 'empty bag' : 'tank') : x.kind.toLowerCase()} ${x.P}kPa ${x.T.toFixed(0)}K ${x.ph}`;
function focus(g, i) {
  if (!i) return '';
  const n = i.nodes;
  const by = f => n.filter(f);
  if (g === 'K1') { const fl = by(x => x.kind === 'JUNC' && x.liq > 0 && x.vap > 0).sort((a, b) => a.P - b.P)[0] || by(x => x.kind === 'JUNC').sort((a, b) => a.P - b.P)[0];
    const caps = i.modes ? i.modes.map((m, k) => m === 'VELOCITY_LIMITED' ? 'e' + k : null).filter(Boolean) : [];
    return (fl ? 'flashing/vented ' + fmt(fl) : '') + (caps.length ? '; capped ' + caps.join(',') : ''); }
  if (g === 'K2') { const j = by(x => x.kind === 'JUNC' && x.P > 1000 && x.T > 150 && x.T < 270 && (x.liq > 0 || x.vap > 0)).sort((a, b) => b.P - a.P)[0]; return j ? 'mixing ' + fmt(j) : ''; }
  if (g === 'K3') { const e = by(empty)[0]; const b = by(x => x.liq > 0 && x.vap > 0 && x.vap / (x.liq + x.vap) < 0.05)[0]; return [e ? 'first arrival in ' + fmt(e) : '', b ? 'bubble point ' + fmt(b) : ''].filter(Boolean).join('; '); }
  if (g === 'K4') { const t = by(x => x.kind === 'RESE' && x.liq >= 0.75 && !empty(x)).sort((a, b) => b.liq - a.liq)[0]; return t ? 'liquid-full ' + fmt(t) + ` liq=${t.liq}` : ''; }
  if (g === 'K6') { const p = i.edges.find(e => /Pump/.test(e.ctl)); return p ? `pump e${p.i} n${p.a}->n${p.b}` + (i.modes ? ' ' + i.modes[p.i] : '') : ''; }
  return '';
}
const header = ['dist', 'seed', 'class', 'blocks', 'heldTick', 'committed', 'island', 'trigger', 'eventBlocks', 'primary', 'resid', 'group', 'evidence', 'atHold', 'islandFeeds', 'islandDevices'];
console.log(header.join('\t'));
const rows = [];
for (const [dist, path] of [['cold', rawC], ['bundled', rawB]])
  for (const l of fs.readFileSync(path, 'utf8').trim().split(/\r?\n/)) {
    if (l.startsWith('dist\t')) continue;
    const r = l.split('\t'); const [, seed, cls, blocks, held, committed, island, trigger, eventBlocks, primary, resid, , rej, feeds, devs] = r;
    const key = dist + '/' + seed, i = ISL[dist][seed];
    let g, ev;
    if (TRACED[key]) { g = TRACED[key]; ev = 'trace (t37)'; }
    else if (PRIOR[key]) { g = PRIOR[key][0]; ev = 'prior: ' + PRIOR[key][1]; }
    else if (SIGNATURE[key]) { g = SIGNATURE[key]; ev = 'signature'; }
    else {
      ev = 'signature';
      const rr = resid === '' ? NaN : +resid, mixing = /LNG/.test(feeds) && /CO2gas/.test(feeds);
      const liquidFull = i && i.nodes.some(x => x.kind === 'RESE' && x.liq >= 0.98);
      if (rr < 1e-4) g = 'K3';
      else if (liquidFull && !(mixing && (isNaN(rr) || rr < 1))) g = 'K4';
      else if (mixing && (isNaN(rr) || rr < 1)) g = 'K2';
      else g = 'K1';
    }
    rows.push({ g, line: [dist, seed, cls, blocks, held, committed, island, trigger, eventBlocks, primary, resid, g, ev, focus(g, i) || (i ? '' : '(no island print)'), feeds, devs].join('\t') });
  }
rows.push({ g: 'K4', line: ['rig', 'H4', 'E5', '-', '1675', '-', 'row 10 (R1)', 'refresh', 'water into liquid-full CH4 tank', 'Newton line search stalled at |r|', '42', 'K4', 'signature (TEST_WORLD_RIG_HOLDS_TRACE)', 'liquid-full CH4 tank + 9.9 kg ice at 125.29 K, 956.2 kPa', 'water 298 K 1 MPa', 'tank'].join('\t') });
rows.push({ g: 'K1', line: ['rig', 'H5', 'E7', '-', '1969 (98.5 s)', '-', 'row 5 (R8)', 'interval', 'blowdown', 'Phase/device active-set cycle', '', 'K1', 'signature (DECISION_LOG)', 'compressor between a slack frosted tank (N2+10 % CO2, 153 K, 1 atm) and Tb at ratio 3', 'N2+CO2 160 K 300 kPa', 'tank x2, compressor'].join('\t') });
for (const r of rows) console.log(r.line);
const count = {};
for (const r of rows) count[r.g] = (count[r.g] || 0) + 1;
console.error(Object.keys(GROUP).map(g => `${g} ${count[g] || 0}  ${GROUP[g]}`).join('\n'));

// t13 (2026-10-13): one row per traced E6 hold (src-t13/T13TraceProbeTest trace files, instr-t13-failing-rows.patch) with
// the features the mechanism groups are read from. Usage: node t13-classify.js <trace-dir> [--tsv]
// Read from the last iteration-limit report of the hold tick (the smallest step size):
//   when      first = the island's first interval (its committed tick is the one it was formed with); +Nt = N ticks after
//             the island was formed; +Ns = N seconds
//   res       the residual at the limit; row = the worst row (flow = a connection's flow law; wsat = a node's water-vapour
//             saturation; pclose = its partial-pressure closure; frac/continuity/enthalpy = a junction's mixing rows)
//   at        for a flow row: the two ends (G generator, V void, R tank, J junction; g vapour, l oil, w free water), its flow,
//             drive and cap drops; MIXED when the two ends' columns give drives of opposite sign (a pressure difference
//             between the gas head and the liquid head)
//   start     that connection at the pass's start point: its start flow q0, drive d0, row r0 (0.5: a cap-level flow
//             against a drive below the cap), MIXED at the start
//   k5        the geometric mean contraction of the Newton's last five iterations; n0 its first residual; fresh = fresh
//             Jacobians; damped = iterations accepted with a step below one; flips = sign changes of that connection
//   dtdep     the residual at the largest step over the residual at the smallest (1.00: the failure does not depend on dt)
//   forbid    connections flowing in a direction their boundary forbids at the failing iterate (a void supplying, a
//             generator receiving)
const fs = require('fs'), path = require('path');
const [dir] = process.argv.slice(2); const tsv = process.argv.includes('--tsv');
const kindOf = (lines, node) => {
  const l = lines.find(x => x.startsWith(`  node ${node} `)); if (!l) return '?';
  let k = / J /.test(l) ? 'J' : /GENERATOR/.test(l) ? 'G' : /VOID/.test(l) ? 'V' : /PORT/.test(l) ? 'P' : 'R';
  const m = l.match(/ vap=(\S+) liq=(\S+) wat=(\S+) /);
  if (m) k += ':' + (+m[1] > 0 ? 'g' : '') + (+m[2] > 0 ? 'l' : '') + (+m[3] > 0 ? 'w' : '');
  return k;
};
const p3 = x => (Number.isFinite(x) ? x.toPrecision(3) : '-');
const rows = [];
for (const file of fs.readdirSync(dir).filter(f => /^[dr]-\d+(-min)?\.txt$/.test(f))) {
  const text = fs.readFileSync(path.join(dir, file), 'utf8'); const L = text.split(/\r?\n/);
  const [, d, seed] = file.match(/^([dr])-(\d+)/);
  const held = (text.match(/^T13_HELD Held\[tick=(\d+), island=(\d+), committed=(\d+)/m) || []).slice(1).map(Number);
  const formed = (text.match(/^T13_FORMED seen=(\d+) startCommitted=(\d+)/m) || []).slice(1).map(Number);
  const blocks = +(text.match(/blocks=(\d+)/) || [, 0])[1];
  const fulls = text.split(/\nT13_FULL /).slice(1).map(b => b.split(/\r?\n/));
  const passes = L.filter(l => l.startsWith('T13_PASS '));
  const limit = passes.filter(p => /msg=Newton iteration limit/.test(p));
  const resAt = p => +(p.match(/residual (\S+?) /) || [, NaN])[1];
  const other = {};
  for (const p of passes) {
    const msg = (p.match(/msg=([^|]*)/) || [, ''])[1].trim(); if (/iteration limit/.test(msg)) continue;
    const key = (/rate=true/.test(p) ? 'rate:' : '') + (/Singular/.test(msg) ? 'singular' : /line search stalled/.test(msg) ? 'stall' : /No finite/.test(msg) ? 'nofinite' : /cycle/.test(msg) ? 'cycle' : msg.slice(0, 30));
    other[key] = (other[key] || 0) + 1;
  }
  const last = [...fulls].reverse().find(b => /msg=Newton iteration limit/.test(b[0])) || [''];
  const hist = ((last.find(l => l.startsWith('  newton=')) || '').slice(9)).trim().split(/\s+/).filter(Boolean);
  const norms = hist.map(h => parseFloat(h));
  const fresh = hist.slice(1).filter(h => !/^[\d.e+-]+s/.test(h)).length, damped = hist.filter(h => /\/a/.test(h)).length;
  const k5 = norms.length > 5 ? Math.pow(norms[norms.length - 1] / norms[norms.length - 6], 1 / 5) : NaN;
  const rowsLine = last.find(l => l.startsWith('  rows:')) || '';
  const top = rowsLine.match(/\[(flow e(\d+)|control e\d+|filter e\d+|node (\d+) (\S+)) r=(\S+)\]/);
  let row = '?', at = '', start = '', flips = 0, edge = -1;
  if (top && top[2] != null) {
    edge = +top[2]; row = 'flow';
    const pl = last.find(l => l.startsWith(`  pipe ${edge} `)) || '';
    const m = pl.match(/pipe \d+ (\d+)->(\d+) ports=(\S+) (\S+) .*q=(\S+) drive=(\S+) loss=(\S+) scale=(\S+)(?: cap0=(\S+) cap1=(\S+))?/);
    if (m) at = `${kindOf(last, m[1])}>${kindOf(last, m[2])}${m[4] === 'Passive' ? '' : ' ' + m[4]} q=${p3(+m[5])} d=${p3(+m[6])} cap=${m[9] ? p3(+m[9]) + '/' + p3(+m[10]) : '-'}${m[3] !== 'BULK/BULK' ? ' ' + m[3] : ''}${/ dz=(-?\d+)/.test(pl) ? ' dz=' + pl.match(/ dz=(-?\d+)/)[1] : ''}${/ MIXED/.test(pl) ? ' MIXED' : ''}`;
    const st = (last.find(l => l.startsWith('  start:')) || '').match(new RegExp(`\\[e${edge} \\S+ dz=\\S+ q0=(\\S+) d0=(\\S+) l0=\\S+ r0=(\\S+) colRho=\\S+ dA=\\S+ dB=\\S+?( MIXED)?\\]`));
    start = st ? `q0=${p3(+st[1])} d0=${p3(+st[2])} r0=${p3(+st[3])}${st[4] ? ' MIXED' : ''}` : 'r0<.05';
    const fl = (last.find(l => l.startsWith('  flips:')) || '').match(new RegExp(` e${edge}:(\\d+)`)); flips = fl ? +fl[1] : 0;
  } else if (top && top[3] != null) {
    row = top[4].replace(/c\d+/, 'c');
    const nl = last.find(l => l.startsWith(`  node ${top[3]} `)) || '';
    const m = nl.match(/ P=(\S+) T=(\S+) m=(\S+) vap=(\S+) liq=(\S+) wat=(\S+) \| seedP=(\S+) .* n=\{([^}]*)\}/);
    at = kindOf(last, top[3]) + (m ? ` P=${(+m[1]).toFixed(0)} seedP=${(+m[7]).toFixed(0)} vap=${m[4]} wat=${m[6]} n={${m[8].trim().split(' ').slice(0, 3).join(' ')}}` : '');
  } else if (top) row = top[1];
  // forbidden directions at the failing iterate
  const kinds = {}; for (const l of last) { const m = l.match(/^ {2}node (\d+) id=\d+ (\w+)( J)?/); if (m) kinds[m[1]] = m[3] ? 'J' : m[2][0]; }
  let forbid = 0; for (const l of last) { const m = l.match(/^ {2}pipe (\d+) (\d+)->(\d+) .* closed=(\w+) q=(\S+)/); if (!m) continue; const q = +m[5], a = kinds[m[2]], b = kinds[m[3]]; if (m[4] === 'true' || Math.abs(q) <= 1e-10) continue; if (q < 0 && (a === 'G' || b === 'V') || q > 0 && (b === 'G' || a === 'V')) forbid++; }
  const mixed = last.filter(l => / MIXED/.test(l) && l.startsWith('  pipe ')).length;
  let when = '?';
  if (held.length && formed.length) { const ticks = held[2] - formed[1]; when = ticks === 0 ? 'first' : ticks < 20 ? `+${ticks}t` : `+${Math.round(ticks / 20)}s`; }
  const r0 = limit.length ? resAt(limit[0]) : NaN, r1 = limit.length ? resAt(limit[limit.length - 1]) : NaN;
  rows.push({ d: d === 'd' ? 'defaults' : 'random', seed: +seed, file, blocks, tick: held[0], when, res: r1, row, at, start, flips, n0: norms[0], k5, fresh, iters: norms.length - 1, damped, dtdep: r0 / r1, forbid, mixed, other: Object.entries(other).map(([k, v]) => `${k}=${v}`).join(',') });
}
rows.sort((a, b) => a.d.localeCompare(b.d) || a.seed - b.seed);
// The group, in this order (review "Hold class E6 on the final code, by mechanism"):
//   T  the worst row is a water-vapour saturation or partial-pressure closure row: a trace gas phase in free water
//   B  a new island (its first second) whose failing iterate carries a forbidden boundary direction
//   C  the worst row's connection sits on its velocity cap in three quarters of the iterates (signature flag "capped")
//   S  every other flow row: the Newton walks from re-estimated start flows
// Lever outcomes (scratch, src/scratch-t13-levers.patch) are read from the replay summaries given after --levers.
const sigOf = {}; for (const f of fs.readdirSync(dir).filter(f => /^[dr]-\d+\.txt$/.test(f))) { const t = fs.readFileSync(path.join(dir, f), 'utf8'); sigOf[f] = (t.match(/^T13_SIG (.*)$/m) || [, ''])[1]; }
const levers = []; const li = process.argv.indexOf('--levers');
if (li > 0) for (const spec of process.argv.slice(li + 1)) { const [name, file] = spec.split('='); const m = {}; for (const l of fs.readFileSync(file, 'utf8').split(/\r?\n/)) { const p = l.split(' '); if (p.length >= 3) m[p[0][0] + '-' + p[1]] = p[2] === 'CLEAN' ? 'clean' : p[2] === 'SAME' ? 'same' : (p[3] || '').replace('tick=', 't') + ':' + (p[4] || ''); } levers.push([name, m]); }
for (const r of rows) {
  const sig = sigOf[r.file] || ''; const young = r.when === 'first' || /t$/.test(r.when);
  r.group = /^(wsat|pclose)$/.test(r.row) ? 'T' : (/ forbid/.test(sig) && young) ? 'B' : / capped/.test(sig) ? 'C' : 'S';
  const lev = levers.map(([n, m]) => `${n}=${m[r.file.slice(0, 1) + '-' + r.seed] || '?'}`);
  const cols = [r.group, r.d, r.seed, r.blocks, r.tick, r.when, p3(r.res), r.row, r.at, r.start, `flips=${r.flips}`, `n0=${p3(r.n0)}`, `k5=${p3(r.k5)}`, `fresh=${r.fresh}/${r.iters}`, `damped=${r.damped}`, `dtdep=${p3(r.dtdep)}`, `forbid=${r.forbid}`, `mixed=${r.mixed}`, r.other, ...lev];
  if (!process.argv.includes('--md')) console.log(tsv ? cols.join('\t') : cols.join(' | '));
}
if (process.argv.includes('--md')) {
  // The review's table: one row per seed, grouped.
  console.log('| Group | Dist. | Seed | Blocks | Tick | When | Row | At the limit | Residual | Newton (first, k5, fresh) | Start of that connection | 60 it. / L1 / L2 / L3 / all |');
  console.log('|---|---|---|---|---|---|---|---|---|---|---|---|');
  const order = { T: 0, S: 1, B: 2, C: 3 };
  for (const r of [...rows].sort((a, b) => order[a.group] - order[b.group] || a.d.localeCompare(b.d) || a.seed - b.seed)) {
    const lv = levers.map(([, m]) => m[r.file.slice(0, 1) + '-' + r.seed] || '?').map(v => v === 'clean' ? 'clean' : v === 'same' ? 'held' : 'moved').join(' / ');
    console.log(`| ${r.group} | ${r.d === 'defaults' ? 'def' : 'rnd'} | ${r.seed} | ${r.blocks} | ${r.tick} | ${r.when} | ${r.row} | ${r.at.replace(/\|/g, '/')} | ${p3(r.res)} | ${p3(r.n0)}, ${p3(r.k5)}, ${r.fresh}/${r.iters} | ${r.start || '-'} | ${lv} |`);
  }
}
const count = {}; for (const r of rows) { const k = r.group + ' ' + r.d; count[k] = (count[k] || []); count[k].push(r.seed); }
for (const k of Object.keys(count).sort()) console.log(`GROUP ${k} ${count[k].length}: ${count[k].join(' ')}`);

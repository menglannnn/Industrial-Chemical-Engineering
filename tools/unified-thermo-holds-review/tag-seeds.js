// t37 (2026-10-18): physical-situation tags per held seed, from the raw table (layout: event, island feeds, devices) and,
// where the verbose replay printed it, the held island's committed state (islands-<dist>.txt from held-islands.js).
// Usage: node tag-seeds.js <raw.tsv> <islands.txt>   -> seed, class, tags, one line each
const fs = require('fs');
const [rawPath, islPath] = process.argv.slice(2);
const raw = fs.readFileSync(rawPath, 'utf8').trim().split(/\r?\n/).filter(l => !l.startsWith('dist\t')).map(l => l.split('\t'));
const isl = {};
let cur = null;
for (const l of fs.readFileSync(islPath, 'utf8').split(/\r?\n/)) {
  let m;
  if ((m = l.match(/^SEED (\d+)/))) { cur = isl[m[1]] = { nodes: [], edges: [] }; continue; }
  if (!cur) continue;
  if ((m = l.match(/^  n(\d+) (\w+) z=(\S+) (\S+)kPa (\S+)K m=(\S+) (\S+) liq=(\S+) vap=(\S+)/)))
    cur.nodes.push({ kind: m[2], z: +m[3], P: +m[4], T: +m[5], m: +m[6], ph: m[7], liq: +m[8], vap: +m[9] });
  else if ((m = l.match(/^  e\d+ n(\d+)->n(\d+) (\S+).*D=(\d+)mm( blocked=\S+)?/))) cur.edges.push({ a: +m[1], b: +m[2], ctl: m[3], D: +m[4], blocked: !!m[5] });
  else if (l.startsWith('  last modes=')) cur.modes = l;
}
for (const r of raw) {
  const [dist, seed, cls, blocks, held, committed, island, trigger, eventBlocks, primary, resid, pass, rej, feeds, devs] = r;
  const t = [];
  const lng = /LNG/.test(feeds), gas = /CO2gas/.test(feeds);
  if (dist === 'cold') t.push(lng && gas ? 'feed=LNG+CO2gas' : lng ? 'feed=LNG' : gas ? 'feed=CO2gas' : 'feed=none');
  else t.push('feed=' + [...new Set(feeds.split(' ').map(f => f.split('_')[0]))].join('+'));
  t.push(trigger === 'event' ? 'event=' + (eventBlocks.split(' ').map(b => b.replace(/\[.*|[0-9.]+$/g, '')).filter((v, i, a) => a.indexOf(v) === i).join('+')) : 'interval');
  const i = isl[seed];
  if (i) {
    const n = i.nodes;
    const empty = x => x.kind === 'RESE' && Math.abs(x.T - 298.1) < 0.2 && Math.abs(x.P - 101) < 1;
    const sink = n.some(x => x.kind === 'VOID') || n.some(empty);
    if (sink) t.push('sink1atm');
    const jFlash = n.filter(x => x.kind === 'JUNC' && x.liq > 0 && x.vap > 0);
    const heavy = jFlash.filter(x => x.vap / (x.liq + x.vap) > 0.5).length, bub = jFlash.filter(x => x.vap / (x.liq + x.vap) < 0.05).length;
    if (heavy) t.push('flashJ' + heavy); if (bub) t.push('bubbleJ' + bub);
    const vesselBub = n.filter(x => x.kind === 'RESE' && x.liq > 0 && x.vap > 0 && x.vap < 0.05).length; if (vesselBub) t.push('vesselBubble' + vesselBub);
    if (n.some(x => x.kind === 'RESE' && x.liq >= 0.98)) t.push('liquidFullTank');
    if (n.some(x => x.kind === 'RESE' && x.liq > 0 && x.vap > 0 && !empty(x))) t.push('boilingTank');
    if (n.filter(empty).length) t.push('emptyBag' + n.filter(empty).length);
    const hot = n.some(x => x.T > 240 && x.kind !== 'VOID' && !empty(x)), cold = n.some(x => x.T < 180);
    if (hot && cold) t.push('warmMeetsCold');
    const genMax = Math.max(...n.filter(x => x.kind === 'GENE').map(x => x.P), 0), maxP = Math.max(...n.map(x => x.P));
    if (maxP > genMax * 1.02 + 5) t.push('aboveEverySource');
    const backed = n.filter(x => x.kind === 'GENE' && x.ph === 'L').some(g => n.some(y => y.kind !== 'GENE' && y.P > g.P * 1.5));
    if (backed) t.push('LNGsourceBackPressured');
    if (i.edges.some(e => e.blocked)) t.push('blockedLine');
    if (i.modes) { const v = (i.modes.match(/VELOCITY_LIMITED/g) || []).length; if (v) t.push('caps' + v); if (/PUMP_HEAD_LIMIT/.test(i.modes)) t.push('pumpHeadLimit'); if (/INLET_WRONG_PHASE/.test(i.modes)) t.push('pumpInletWrongPhase'); }
    if (i.edges.some(e => /Compressor/.test(e.ctl))) t.push('comp');
    if (i.edges.some(e => /Pump/.test(e.ctl))) t.push('pump');
    if (i.edges.some(e => /PressureValve/.test(e.ctl))) t.push('valve');
    t.push('nodes' + n.length);
  } else t.push('noIslandPrint', 'dev=' + devs.replace(/\[[^\]]*\]/g, '').replace(/ /g, '+'));
  const rr = +resid; t.push(resid === '' ? 'r=-' : rr < 1e-4 ? 'r<1e-4' : rr < 1 ? 'r<1' : 'r>=1');
  console.log([dist, seed, cls, blocks, t.join(' '), rej].join('\t'));
}

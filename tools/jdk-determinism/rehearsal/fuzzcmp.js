// Per-seed comparison of two HoldFuzzTest reports (batch 2026-09-27-jdk-determinism, integration rehearsal).
// Key per seed = clean, or the class and status text of the FIRST hold (tick, island, committed tick left out, as in the
// fuzz-runner equivalence check), or the first exception. Class names as ../../phase-ports-probes/hold-fuzz/classify.sh.
// Usage: node fuzzcmp.js <A-report> <B-report> [--list]
const fs = require('fs');
function cls(s) {
  if (/^HELD \((phases|thermo domain)\)/.test(s)) return 'C'; // a phase hold: its prefix since 2026-10-17, or the old one
  if (/^HELD: wall deadline/.test(s) || /^HELD: soft budget/.test(s)) return 'F';
  if (/Backward-Euler state change #=[0-9]{3}/.test(s)) return 'A';
  if (/Matrix values must be finite/.test(s)) return 'B';
  s = s.replace(/[;,] reasons=.*/, '');
  if (/No finite Jacobian/.test(s)) return 'E10';
  if (/Singular matrix: empty or zero row/.test(s)) return 'G';
  if (/^HELD: Singular Newton Jacobian/.test(s)) return 'D';
  if (/^HELD: Newton iteration limit/.test(s) || /^HELD: Newton line search stalled/.test(s) || /^HELD: Phase\/device active-set cycle/.test(s)) return 'E1';
  if (/^HELD: Substep refinement exhausted: Singular Newton Jacobian/.test(s)) return 'E2';
  if (/partial pressure below the numerical floor/.test(s) || /Partial pressure .* outside the numerical box/.test(s) || /Water-vapor approximation outside/.test(s)) return 'E3';
  if (/equation gate/.test(s)) return 'E4';
  if (/^HELD: Substep refinement exhausted: Newton line search stalled/.test(s)) return 'E5';
  if (/^HELD: Substep refinement exhausted: Newton iteration limit/.test(s)) return 'E6';
  if (/active-set cycle/.test(s)) return 'E7';
  if (/Interval substep limit/.test(s)) return 'E8';
  return 'E9';
}
function load(f) {
  const m = new Map(); let seed = null, rec = null;
  for (const raw of fs.readFileSync(f, 'utf8').split('\n')) {
    const l = raw.replace(/\r$/, '');
    let g;
    if ((g = /^SEED (\d+) .*holds=(\d+) classes=\[([^\]]*)\]/.exec(l))) {
      seed = +g[1]; rec = { holds: +g[2], first: null, cls: 'clean', exc: null }; m.set(seed, rec);
    } else if (rec && /^  HELD /.test(l) && rec.first === null) {
      const s = l.replace(/.*status=/, ''); rec.first = s; rec.cls = cls(s);
      const tk = /tick=(\d+)/.exec(l); rec.tick = tk ? +tk[1] : -1;
    } else if (rec && /^  EXCEPTION /.test(l) && rec.exc === null) {
      rec.exc = l.trim(); if (rec.first === null) { rec.cls = 'X'; rec.first = rec.exc; }
    }
  }
  return m;
}
const A = load(process.argv[2]), B = load(process.argv[3]), list = process.argv.includes('--list');
const t = { same: 0, cleanToHeld: [], heldToClean: [], classChange: [], statusChange: [], numericOnly: [], sameTick: [] };
for (const [s, a] of A) {
  const b = B.get(s); if (!b) continue;
  const ka = a.cls === 'clean' ? 'clean' : a.first, kb = b.cls === 'clean' ? 'clean' : b.first;
  if (ka === kb) { t.same++; continue; }
  if (a.cls === 'clean') t.cleanToHeld.push(`${s}:${b.cls}`);
  else if (b.cls === 'clean') t.heldToClean.push(`${s}:${a.cls}`);
  else if (a.cls !== b.cls) t.classChange.push(`${s}:${a.cls}>${b.cls}`);
  else {
    t.statusChange.push(`${s}:${a.cls}`);
    const mask = x => x.replace(/[-+]?(?:\d+\.?\d*|\.\d+)(?:[eE][-+]?\d+)?/g, '#');
    if (mask(a.first) === mask(b.first)) t.numericOnly.push(`${s}:${a.cls}`);
    if (a.tick === b.tick) t.sameTick.push(`${s}:${a.cls}`);
  }
}
const count = m => { const c = {}; for (const r of m.values()) c[r.cls] = (c[r.cls] || 0) + 1; return c; };
const fmt = c => Object.keys(c).sort().map(k => `${k}=${c[k]}`).join(' ');
const hist = arr => { const c = {}; for (const x of arr) { const k = x.split(':')[1]; c[k] = (c[k] || 0) + 1; } return fmt(c); };
console.log(`seeds A=${A.size} B=${B.size} identical first outcome=${t.same}`);
console.log(`A: ${fmt(count(A))}`); console.log(`B: ${fmt(count(B))}`);
console.log(`clean->held ${t.cleanToHeld.length} [${hist(t.cleanToHeld)}]${list ? ' ' + t.cleanToHeld.join(' ') : ''}`);
console.log(`held->clean ${t.heldToClean.length} [${hist(t.heldToClean)}]${list ? ' ' + t.heldToClean.join(' ') : ''}`);
console.log(`held, other class ${t.classChange.length} [${hist(t.classChange)}]${list ? ' ' + t.classChange.join(' ') : ''}`);
console.log(`held, same class, other first status ${t.statusChange.length} [${hist(t.statusChange)}]${list ? ' ' + t.statusChange.join(' ') : ''}`);
console.log(`  of which the status differs only in numbers ${t.numericOnly.length} [${hist(t.numericOnly)}]; first hold at the same tick ${t.sameTick.length} [${hist(t.sameTick)}]`);

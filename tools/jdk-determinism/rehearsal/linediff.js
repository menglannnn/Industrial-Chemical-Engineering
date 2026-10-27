// Per-line comparison of two junction-line files (LIQUID_JUNCTION / MIXED_GAS_* lines): key = kind + interval + case,
// every numeric token compared; prints the lines that differ with the largest relative difference of any token.
// Usage: node linediff.js <old> <new>   (ms, bytes, allocatedMB ignored). Batch 2026-09-27-jdk-determinism, rehearsal.
const fs = require('fs');
const norm = s => s.replace(/\r/g, '').replace(/ ms=[0-9.*]+/, '').replace(/,? bytes=[0-9*]+/, '').replace(/ allocatedMB=[0-9.*]+/, '');
function load(f) {
  const m = new Map();
  for (let l of fs.readFileSync(f, 'utf8').split('\n')) {
    const i = l.search(/(MIXED_GAS_(STATIC|TRANSIENT|COST)|LIQUID_JUNCTION) /);
    if (i < 0) continue;
    l = norm(l.slice(i)).trim();
    const key = l.split(' ').slice(0, 3).join(' ');
    let k = key, n = 1;
    while (m.has(k)) k = key + '#' + (++n);
    m.set(k, l);
  }
  return m;
}
const num = /[-+]?(?:\d+\.?\d*|\.\d+)(?:[eE][-+]?\d+)?|NaN|Infinity/g;
const a = load(process.argv[2]), b = load(process.argv[3]);
const verbose = process.argv.includes('-v');
let same = 0, diff = 0, worst = 0;
for (const [k, la] of a) {
  const lb = b.get(k);
  if (lb === undefined) { console.log('MISSING in new: ' + k); continue; }
  if (la === lb) { same++; continue; }
  diff++;
  const na = la.match(num) || [], nb = lb.match(num) || [];
  if (la.replace(num, '#') !== lb.replace(num, '#') || na.length !== nb.length) {
    console.log('TEXT ' + k); console.log('  old ' + la); console.log('  new ' + lb); continue;
  }
  let r = 0, where = '', ints = [];
  for (let i = 0; i < na.length; i++) {
    if (na[i] === nb[i]) continue;
    const x = Number(na[i]), y = Number(nb[i]);
    if (/^[-+]?\d+$/.test(na[i]) && /^[-+]?\d+$/.test(nb[i])) ints.push(na[i] + '->' + nb[i]);
    const d = (isNaN(x) || isNaN(y)) ? Infinity : Math.abs(x - y) / Math.max(Math.abs(x), Math.abs(y), 1e-300);
    if (d > r) { r = d; where = na[i] + ' -> ' + nb[i]; }
  }
  worst = Math.max(worst, r);
  console.log(`DIFF maxrel=${r.toExponential(3)} (${where})${ints.length ? ' integers ' + ints.join(',') : ''} ${k}`);
  if (verbose) { console.log('  old ' + la); console.log('  new ' + lb); }
}
for (const k of b.keys()) if (!a.has(k)) console.log('MISSING in old: ' + k);
console.log(`same=${same} differ=${diff} worstRel=${worst.toExponential(3)}`);

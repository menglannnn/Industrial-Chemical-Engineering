// Markdown table of the junction lines that differ between two files: every differing token with old and new value.
// Usage: node linetable.js <old> <new>
const fs = require('fs');
const norm = s => s.replace(/\r/g, '').replace(/ ms=[0-9.*]+/, '').replace(/,? bytes=[0-9*]+/, '').replace(/ allocatedMB=[0-9.*]+/, '');
function load(f) {
  const m = new Map();
  for (let l of fs.readFileSync(f, 'utf8').split('\n')) {
    const i = l.search(/(MIXED_GAS_(STATIC|TRANSIENT|COST)|LIQUID_JUNCTION) /);
    if (i < 0) continue;
    l = norm(l.slice(i)).trim();
    m.set(l.split(' ').slice(0, 3).join(' ').replace(/^MIXED_GAS_TRANSIENT (\S+) Outcome\[key=([^,]+),/, 'MGT $1 $2'), l);
  }
  return m;
}
const tok = /([A-Za-z()0-9./-]*?)=?([-+]?(?:\d+\.?\d*|\.\d+)(?:[eE][-+]?\d+)?|NaN|Infinity)/g;
const a = load(process.argv[2]), b = load(process.argv[3]);
console.log('| line | quantity | old | new | relative difference |');
console.log('|---|---|---|---|---|');
for (const [k, la] of a) {
  const lb = b.get(k); if (!lb || la === lb) continue;
  const wa = la.split(/[ ,\[\]]+/), wb = lb.split(/[ ,\[\]]+/);
  const rows = [];
  for (let i = 0; i < Math.min(wa.length, wb.length); i++) {
    if (wa[i] === wb[i]) continue;
    const [na, va] = wa[i].includes('=') ? [wa[i].slice(0, wa[i].lastIndexOf('=')), wa[i].slice(wa[i].lastIndexOf('=') + 1)] : ['perSlice', wa[i]];
    const vb = wb[i].includes('=') ? wb[i].slice(wb[i].lastIndexOf('=') + 1) : wb[i];
    const x = Number(va), y = Number(vb);
    const r = (isNaN(x) || isNaN(y)) ? 'n/a' : (Math.abs(x - y) / Math.max(Math.abs(x), Math.abs(y))).toExponential(2);
    rows.push(`| ${k} | ${na} | ${va} | ${vb} | ${r} |`);
  }
  console.log(rows.join('\n'));
}

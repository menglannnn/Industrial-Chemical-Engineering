// t17 (2026-10-13): the 33 junction lines (LIQUID_JUNCTION, MIXED_GAS_TRANSIENT, MIXED_GAS_COST) of several G1 runs
// side by side, one markdown row per line and field that differs between any two runs (ms, bytes, allocatedMB masked).
// Usage: node t17-junction-table.js name=<junction-lines.txt> [name=<file> ...]
const fs = require('fs');
const runs = process.argv.slice(2).map(a => { const k = a.indexOf('='); return [a.slice(0, k), a.slice(k + 1)]; });
const parse = file => {
  const lines = new Map();
  for (const raw of fs.readFileSync(file, 'utf8').split(/\r?\n/)) {
    if (!raw.trim()) continue;
    let key, fields = {};
    let m;
    if ((m = raw.match(/^MIXED_GAS_TRANSIENT interval=(\S+) Outcome\[key=([^,]+), (.*)\]$/))) {
      key = `MIXED ${m[1]} ${m[2]}`;
      for (const part of m[3].split(', ')) { const [k, v] = part.split('='); fields[k] = v; }
    } else if ((m = raw.match(/^LIQUID_JUNCTION interval=(\S+) (\S+) (.*)$/))) {
      key = `LIQUID ${m[1]} ${m[2]}`;
      for (const part of m[3].match(/\S+=(\[[^\]]*\]|\S+)/g) || []) { const k = part.indexOf('='); fields[part.slice(0, k)] = part.slice(k + 1); }
    } else if ((m = raw.match(/^MIXED_GAS_COST (.*)$/))) {
      key = 'COST';
      for (const part of m[1].split(' ')) { const [k, v] = part.split('='); fields[k] = v; }
    } else continue;
    for (const k of ['ms', 'bytes', 'allocatedMB']) delete fields[k];
    lines.set(key, fields);
  }
  return lines;
};
const data = runs.map(([n, f]) => [n, parse(f)]);
const keys = [...new Set(data.flatMap(([, d]) => [...d.keys()]))];
console.log(`| line | field | ${data.map(([n]) => n).join(' | ')} |`);
console.log(`|---|---|${data.map(() => '---').join('|')}|`);
let moved = 0;
for (const key of keys) {
  const fieldNames = [...new Set(data.flatMap(([, d]) => Object.keys(d.get(key) || {})))];
  let any = false;
  for (const f of fieldNames) {
    const values = data.map(([, d]) => (d.get(key) || {})[f] ?? '-');
    if (new Set(values).size > 1) { any = true; console.log(`| ${key} | ${f} | ${values.join(' | ')} |`); }
  }
  if (any) moved++;
}
console.log(`\nlines: ${keys.length}, lines with a difference: ${moved}`);

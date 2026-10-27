// Compares two printed-line files of xsuite.sh line by line after sorting the entries inside every "reasons={...}" map
// (PassiveIntervalSolver.Result keeps its rejection reasons in Map.copyOf, whose iteration order is randomised per JVM start).
// Prints the lines that still differ and the count of lines that differed only in that order.
// Usage: node printeddiff.js <a> <b>
const fs = require('fs');
const read = f => fs.readFileSync(f, 'utf8').replace(/\r/g, '').split('\n');
const norm = l => l.replace(/reasons=\{([^}]*)\}/g, (m, inner) => 'reasons={' + inner.split(/, (?=[^,]*=\d+(?:, |$))/).sort().join(', ') + '}');
const a = read(process.argv[2]), b = read(process.argv[3]);
let orderOnly = 0, differ = 0;
if (a.length !== b.length) console.log(`line counts differ: ${a.length} vs ${b.length}`);
for (let i = 0; i < Math.min(a.length, b.length); i++) {
  if (a[i] === b[i]) continue;
  if (norm(a[i]) === norm(b[i])) { orderOnly++; continue; }
  differ++;
  if (differ <= 20) { console.log(`line ${i + 1}:`); console.log('  < ' + a[i].slice(0, 400)); console.log('  > ' + b[i].slice(0, 400)); }
}
console.log(`lines=${a.length} identical=${a.length - orderOnly - differ} reasonsOrderOnly=${orderOnly} differ=${differ}`);

const fs = require('fs');
const read = f => fs.readFileSync(f, 'utf8').replace(/\r/g, '').split('\n').filter(l => l.startsWith('WP7c grid network'));
const a = read(process.argv[2]), b = read(process.argv[3]);
let same = 0, bad = 0;
for (let i = 0; i < Math.max(a.length, b.length); i++) {
  const la = a[i] || '', lb = b[i] || '';
  if (la === lb) { same++; continue; }
  // An isotherm line: the port's must equal the branch's with the 5 and 10 MPa columns removed.
  const strip = la.replace(/ 5\.0 MPa \S+ 10\.0 MPa \S+/, '');
  if (strip === lb) { same++; continue; }
  bad++; console.log('A: ' + la + '\nB: ' + lb);
}
console.log({lines: b.length, matching: same, different: bad});

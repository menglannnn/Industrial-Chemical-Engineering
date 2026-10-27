// Checks every relative markdown link in the generated tree; a target may exist in the generated tree or in the source tree.
// Usage: node check.js <generated documentation dir> <source documentation dir>
const fs = require('fs');
const path = require('path');
const [gen, src] = process.argv.slice(2);
let links = 0; const bad = [];
function walk(d) {
  for (const f of fs.readdirSync(d)) {
    const p = path.join(d, f);
    if (fs.statSync(p).isDirectory()) walk(p);
    else if (f.endsWith('.md')) check(p);
  }
}
function check(file) {
  const text = fs.readFileSync(file, 'utf8');
  const rel = path.relative(gen, path.dirname(file));
  for (const m of text.matchAll(/\]\(([^)\s]+)\)/g)) {
    const t = m[1];
    if (/^[a-z]+:|^#/i.test(t)) continue;
    links++;
    const target = decodeURI(t.split('#')[0]);
    const inGen = path.resolve(gen, rel, target);
    const inSrc = path.resolve(src, rel, target);
    if (!fs.existsSync(inGen) && !fs.existsSync(inSrc)) bad.push(path.relative(gen, file) + ' -> ' + t);
  }
}
walk(gen);
console.log(links + ' links, ' + bad.length + ' broken');
for (const b of bad.slice(0, 60)) console.log('  ' + b);

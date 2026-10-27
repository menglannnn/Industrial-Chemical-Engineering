const fs = require('fs');
const d = process.argv[2];
let t = 0, f = 0, e = 0, s = 0;
const want = process.argv.slice(3);
for (const n of fs.readdirSync(d)) {
    if (!n.endsWith('.xml')) continue;
    const x = fs.readFileSync(d + '/' + n, 'utf8');
    const m = x.match(/<testsuite\b[^>]*>/)[0];
    const g = k => { const r = m.match(new RegExp(' ' + k + '="([0-9]+)"')); return r ? +r[1] : 0; };
    t += g('tests'); f += g('failures'); e += g('errors'); s += g('skipped');
    for (const w of want) if (n.includes(w)) console.log(n.replace(/^TEST-com\.wormzjl\.icheme\./, ''), 'tests', g('tests'), 'failures', g('failures'), 'errors', g('errors'));
}
console.log('total tests', t, 'failures', f, 'errors', e, 'skipped', s);

// t37 (2026-10-18): the held island of each seed's first hold, from a verbose HoldFuzzTest replay (-PholdFuzzVerbose=true):
// its committed node states (kind, z, P, T, mass, phase volumes) and lines (bore, control, blocked, ports, last flow), one
// compact block per seed. Usage: node held-islands.js <gradle log> [seed ...]
const fs = require('fs');
const [log, ...want] = process.argv.slice(2);
const L = fs.readFileSync(log, 'utf8').split(/\r?\n/);
const seen = new Set();
const g = (s, k) => { const m = s.match(new RegExp(k + '=([^ ]+)')); return m ? m[1] : ''; };
const kPa = p => (p / 1000).toFixed(p < 1e4 ? 2 : 0);
for (let i = 0; i < L.length; i++) {
  const m = L[i].match(/FUZZ_VERBOSE_HELD seed=(\d+) Held\[tick=(\d+), island=(\d+), committed=(\d+), status=(.*)\]$/);
  if (!m || seen.has(m[1]) || (want.length && !want.includes(m[1]))) continue;
  seen.add(m[1]);
  const out = [`SEED ${m[1]} held=${m[2]} island=${m[3]} committed=${m[4]} :: ${m[5].replace(/; reasons=.*/, '').slice(0, 110)}`];
  for (let j = i + 2; j < L.length; j++) {
    const l = L[j].trim();
    if (l.startsWith('node ')) {
      const kind = l.split(' ')[3], P = +g(l, 'P'), T = +g(l, 'T'), mass = +g(l, 'm'), liq = +g(l, 'liq'), wat = +g(l, 'water'), vap = +g(l, 'vap');
      const ph = (liq > 0 ? 'L' : '') + (wat > 0 ? 'W' : '') + (vap > 0 ? 'V' : '') || '-';
      out.push(`  n${l.split(' ')[1]} ${kind.slice(0, 4)} z=${g(l, 'z')} ${kPa(P)}kPa ${T.toFixed(1)}K m=${mass.toPrecision(3)} ${ph} liq=${liq.toPrecision(2)} vap=${vap.toPrecision(2)}`);
    } else if (l.startsWith('pipe ')) {
      const p = l.match(/^pipe (\d+) (\d+)->(\d+) (\S+(?:\[[^\]]*\])?) d=(\S+) blocked=(\S+) filter=(\S+) ports=(\S+)/);
      if (p) out.push(`  e${p[1]} n${p[2]}->n${p[3]} ${p[4].replace(/\[\]$/, '')} D=${(Math.sqrt(+p[5] * 4 / Math.PI) * 1000).toFixed(0)}mm${p[6] !== '0' ? ' blocked=' + p[6] : ''}${p[7] === 'true' ? ' filter' : ''} ${p[8]}`);
      else out.push('  ' + l.slice(0, 160));
    } else if (l.startsWith('last ')) out.push('  ' + l.slice(0, 400));
    else break;
  }
  console.log(out.join('\n'));
}

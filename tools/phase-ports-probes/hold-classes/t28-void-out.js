// t28: scans a raw pass trace (src/instr-t28-pass-trace-on-84787c80.patch, -Dt19trace=<file>) for flow out of a void or
// into a generator: every PASS start, FAIL iterate and CONV point whose line names a connection with a void at its first
// end carrying q > 1e-10 kg/s (or at its second end, q < -1e-10), or a generator receiving likewise. Prints the trace
// line number, the line's kind and pass, the solve's dt, and each forbidden flow with the junction pressure it faces.
// Usage: node t28-void-out.js <raw trace> [--first N]   (only the first N solves, counted by "PASS 0")
const fs = require('fs');
const lines = fs.readFileSync(process.argv[2], 'utf8').split(/\r?\n/);
const k = process.argv.indexOf('--first'); const firstN = k > 0 ? +process.argv[k + 1] : Infinity;
let solve = 0, dt = '?', shown = 0;
for (let i = 0; i < lines.length; i++) {
  const l = lines[i];
  const pm = l.match(/ PASS (\d+) dt=(\S+)/); if (pm) { if (pm[1] === '0') solve++; dt = pm[2]; }
  if (solve > firstN) break;
  const kind = (l.match(/ (PASS \d+|FAIL pass=\d+|CONV pass=\d+)/) || [])[1]; if (!kind) continue;
  const nodes = {}; for (const m of l.matchAll(/n(\d+):([A-Z]) P=(\S+)/g)) nodes[m[1]] = { kind: m[2], p: +m[3] };
  const bad = [];
  for (const m of l.matchAll(/\| e(\d+) (\d+)>(\d+) (\S+) q=(\S+)/g)) {
    const a = nodes[m[2]], b = nodes[m[3]], q = +m[5]; if (!a || !b) continue;
    const outOfVoid = a.kind === 'V' && q > 1e-10 || b.kind === 'V' && q < -1e-10;
    const intoGen = b.kind === 'G' && q > 1e-10 || a.kind === 'G' && q < -1e-10;
    if (outOfVoid || intoGen) bad.push(`e${m[1]} ${m[2]}>${m[3]} ${outOfVoid ? 'out of void' : 'into generator'} q=${q} (P ${a.p} > ${b.p})`);
  }
  if (bad.length) { console.log(`line ${i + 1} solve ${solve} dt=${dt} ${kind}: ${bad.join('; ')}`); if (++shown > 60) break; }
}
console.log(`solves scanned: ${solve}`);

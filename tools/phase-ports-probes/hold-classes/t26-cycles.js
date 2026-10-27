// t26: the first step solve (rate=false) of a pass trace (t26-trace.sh, src/instr-t26-pass-trace.patch) that ends in an
// active-set cycle, as one line per pass: the cap states, the device modes, the node phase codes, and why the pass ended.
// Usage: node t26-cycles.js <raw trace> [--rate] [--all]   (--rate: also rate solves; --all: every cycling solve, not the first)
const fs = require('fs');
const [file, ...flags] = process.argv.slice(2);
const lines = fs.readFileSync(file, 'utf8').split(/\r?\n/).map(l => l.replace(/^\S+ /, ''));
let solve = null; const solves = [];
for (const l of lines) {
  if (/^PASS 0 /.test(l)) { solve = { head: l, passes: [] }; solves.push(solve); }
  if (!solve) continue;
  if (/^PASS /.test(l)) {
    const g = k => (l.match(new RegExp(' ' + k + '=(\\[[^\\]]*\\]|\\S+)')) || [])[1];
    const nodes = (l.split(' start: ')[1] || '').split(' | ')[0];
    solve.passes.push({ n: +l.split(' ')[1], dt: g('dt'), rate: g('rate'), caps: g('caps'), modes: (g('modes') || '').replace(/PUMP_|VALVE_/g, '').replace(/PASSIVE/g, 'P'),
      phases: g('phases'), emptying: g('emptying'), closed: g('closed'), nodes, why: [] });
  } else if (/^(FAIL|WHY|CONV|CYCLE)/.test(l)) {
    const p = solve.passes[solve.passes.length - 1]; if (!p) continue;
    if (/^FAIL/.test(l)) p.why.push('FAIL ' + (l.match(/(line search stalled|iteration limit|[A-Za-z ]+?) at residual (\S+)/) || []).slice(1).join(' ') + ' @' + (l.split(' at: ')[1] || '').split(' | ')[0]);
    else if (/^CONV/.test(l)) p.why.push(l.split(' at: ')[0]);
    else p.why.push(l.split(' at: ')[0]);
  }
}
const pick = solves.filter(s => s.passes.some(p => p.why.some(w => /^CYCLE/.test(w))) && (flags.includes('--rate') || / rate=false/.test(s.head)));
// --min: the cycling step solve at the smallest dt (the held interval's refinement reaches it), its first occurrence.
const byDt = pick.slice().sort((a, b) => +a.passes[0].dt - +b.passes[0].dt);
for (const s of flags.includes('--all') ? pick : flags.includes('--min') ? byDt.slice(0, 1) : pick.slice(0, 1)) {
  console.log('SOLVE dt=' + s.passes[0].dt + ' rate=' + s.passes[0].rate + ' closed=' + s.passes[0].closed);
  for (const p of s.passes) console.log(`  pass ${p.n} caps=${p.caps} modes=${p.modes} phases=${p.phases}${p.emptying !== 'null' ? ' emptying=' + p.emptying : ''}\n     start ${p.nodes}\n     ${p.why.join('\n     ')}`);
}
console.log(`solves=${solves.length} step solves=${solves.filter(s => / rate=false/.test(s.head)).length} cycling step solves=${solves.filter(s => / rate=false/.test(s.head) && s.passes.some(p => p.why.some(w => /^CYCLE/.test(w)))).length}`);

// winsweep comparison: reads <out>/r1..rN (r1 = reference runtime), prints two Markdown tables (verdict, evidence) to
// stdout and <out>/table.md, details to <out>/details.txt, and exits 1 on any real difference: printed suite lines,
// failing tests, chain-100 (tiered or forced-compiled), or fuzz first holds that differ without a budget hold on either
// side.   node compare.js <out-dir> <N>
const fs = require('fs'), path = require('path');
const [out, nArg] = process.argv.slice(2); const N = +nArg;
const rd = (d, f) => { const p = path.join(out, d, f); return fs.existsSync(p) ? fs.readFileSync(p, 'utf8').replace(/\r/g, '') : null; };
const details = [];

function identity(d) {
  const t = rd(d, 'identity.txt') || '';
  const prop = k => { const m = new RegExp('^\\s*' + k.replace(/\./g, '\\.') + ' = (.*)$', 'm').exec(t); return m ? m[1].trim() : ''; };
  const vm = prop('java.vm.name'), ver = prop('java.runtime.version'), vendor = prop('java.vendor.version') || prop('java.vendor');
  const major = parseInt(prop('java.specification.version'), 10);
  const graal = /UseJVMCICompiler\s+=\s+true/.test(t), openj9 = /OpenJ9/.test(vm + prop('java.vm.vendor'));
  const family = openj9 ? `OpenJ9 ${major}` : graal ? `GraalVM ${major} (Graal JIT)` : `HotSpot ${major}`;
  return { name: `${vendor} ${ver}`.trim(), family, graal, openj9 };
}

// JIT evidence of the suite run. jit.log (-Xlog:jit+compilation, HotSpot family): "[uptime s][level] id attrs tier method".
const FACADE = ['DeterministicMath::log', 'DeterministicMath::exp', 'DeterministicMath::cbrt', 'DeterministicMath::pow', 'DeterministicPow::pow'];
function jit(d, id) {
  if (id.openj9) return 'OpenJ9 JIT (no log; forced run: -Xjit:count=0)';
  const t = rd(d, 'jit.log');
  if (!t) return 'no JIT log';
  let end = 0, top = 0, sci = 0; const first = {};
  for (const l of t.split('\n')) {
    const m = /^\[([\d.]+)s\]\[\w+\]\s+\d+\s+(?:[%sbn!]+\s+)?(\d)\s+(\S+)/.exec(l);
    if (!m) continue;
    end = Math.max(end, +m[1]);
    if (m[2] !== '4' || /made not entrant/.test(l)) continue;
    top++;
    if (m[3].startsWith('com.wormzjl.createcheme.science.')) sci++;
    const short = m[3].replace(/^com\.wormzjl\.createcheme\.science\.math\./, '');
    if (FACADE.includes(short) && !(short in first)) first[short] = +m[1];
  }
  const compiler = id.graal ? 'Graal' : 'C2';
  const fac = FACADE.map(f => `${f.replace('::', '.').replace('Deterministic', 'D')} ${f in first ? (100 * first[f] / end).toFixed(0) + '%' : 'not'}`).join(', ');
  let g = '';
  if (id.graal) {
    const log = rd(d, 'graal-compilation.log') || '';
    const lines = log.split('\n').filter(l => /^HotSpot(OSR)?Compilation-\d+/.test(l));
    g = `; Graal log: ${lines.length} compilations, ${lines.filter(l => /^\S+\s+Lcom\/wormzjl\/createcheme\/science\//.test(l)).length} of science/`;
  }
  return `tier 4 = ${compiler}: ${top} compilations, ${sci} of science/${g}; facade at tier 4 after (share of the run): ${fac}`;
}

// MathSweep: functions where Math != StrictMath on this runtime, and functions whose Math results differ from the reference's.
function mathsweep(d) {
  const t = rd(d, 'mathsweep.txt') || ''; const fns = {};
  for (const l of t.split('\n')) {
    const w = l.trim().split(/\s+/);
    if (w.length >= 7 && /^[a-z0-9]+$/.test(w[0]) && /^\d+$/.test(w[1])) fns[w[0]] = { differ: +w[1], hash: w[3], stable: w[6] !== 'NO' };
  }
  return fns;
}
const normReasons = l => l.replace(/reasons=\{([^}]*)\}/g, (m, inner) => 'reasons={' + inner.split(/, (?=[^,]*=\d+(?:, |$))/).sort().join(', ') + '}');
function suite(d) {
  const t = rd(d, 'suite.txt'); if (!t) return null;
  const all = t.split('\n');
  const cut = all.findIndex(l => /^Failures \(\d+\):/.test(l) || /^MINILAUNCHER /.test(l));
  const body = cut < 0 ? all : all.slice(0, cut);
  const summary = (all.find(l => /^MINILAUNCHER /.test(l)) || '').replace(/ wallMs=\d+/, '');
  const failSet = [...new Set(all.filter(l => /^\s+JUnit Jupiter:/.test(l)).map(l => l.trim()))].sort();
  const printed = [], timing = []; let frames = 0, jvmWarnings = 0;
  for (let l of body) {
    if (/^\[Use -Djdk\.graal/.test(l)) continue;   // GraalVM's notice that its log goes to a file
    // JDK-internal stack frames: their count and line numbers belong to the JDK build, not to the product.
    if (/^\s+at (?:(?:java|jdk)\.[\w.]+\/|(?:java|javax|jdk|sun|com\.sun)\.)/.test(l) || /^\s+\.\.\. \d+ more/.test(l)) { frames++; continue; }
    // The JVM's own warnings (JDK 24+: a library called a terminally deprecated sun.misc.Unsafe method), printed mid-run.
    if (/^WARNING: /.test(l)) { jvmWarnings++; continue; }
    l = l.replace(/^\[\d\d:\d\d:\d\d(?:\.\d+)?\]/, '[clock]');   // Log4j time of day
    // Log4j marks a frame's jar "~[...]" when it guessed the class location; on JDK 25 it resolves it exactly ("[...]").
    if (/^\s+at /.test(l)) l = l.replace(/ ~\[/, ' [');
    const raw = l;
    l = l.replace(/ ms=[0-9.]+/g, ' ms=*').replace(/ allocatedMB=[0-9.]+/g, ' allocatedMB=*').replace(/ bytes=[0-9]+/g, ' bytes=*')
      .replace(/wall(Ms|Nanos)?=[0-9.]+/g, 'wall=*').replace(/\b\d+(?:\.\d+)? ?ms\b(?!=)/g, '*ms').replace(/Nanos=\d+/g, 'Nanos=*');
    // A line of times without units on the numbers (a median table) is dropped; a line whose times were masked is kept.
    if (/median ms/.test(l)) { timing.push(raw); continue; }
    if (l !== raw && /\*ms|Nanos=\*/.test(l)) timing.push(raw);
    printed.push(l);
  }
  return { summary, failSet, printed, timing, frames, jvmWarnings };
}
function chain(d, f) {
  const t = rd(d, f) || '';
  const dev = (/chain-100 vs reference: (.*)/.exec(t) || [, 'no chain-100 line'])[1].trim();
  const row = (/^\s*\| 0 \|(.*)$/m.exec(t) || [, ''])[1].split('|').map(s => s.trim());
  const counts = row.length > 10 ? `accepted=${row[2]} rejected=${row[3]} solves=${row[6]} iterations=${row[7]} residualEvaluations=${row[9]}` : 'no table row';
  const diffs = t.split('\n').filter(l => /^chain-100 interval /.test(l)).join('\n');
  const g = k => (new RegExp(k + ' (\\S+)').exec(dev) || [, '?'])[1];
  return { dev, counts, diffs, short: `moles ${g('state/moles')}, T ${g('temperature')} K, flow ${g('flow')}` };
}
const sameChain = (a, b) => a.dev === b.dev && a.counts === b.counts && a.diffs === b.diffs;
const fcls = s => /^HELD: wall deadline/.test(s) || /^HELD: soft budget/.test(s) ? 'F' : 'other';
function fuzz(d, dist) {
  const t = rd(d, `fuzz-${dist}-report.txt`); if (!t) return null;
  const m = new Map(); let rec = null, clean = 0;
  for (const l of t.split('\n')) {
    let g;
    if ((g = /^SEED (\d+) /.exec(l))) { rec = { first: 'clean', cls: 'clean' }; m.set(+g[1], rec); }
    else if (rec && rec.first === 'clean' && /^  (HELD|EXCEPTION) /.test(l)) {
      const held = /^  HELD /.test(l), s = held ? l.replace(/.*status=/, '') : l.trim();
      rec.first = s; rec.cls = held ? fcls(s) : 'X';
    }
  }
  for (const r of m.values()) if (r.cls === 'clean') clean++;
  return { m, clean };
}
function fuzzCmp(a, b) {
  if (!a || !b) return { text: 'not run', real: [] };
  let same = 0; const budget = [], real = [];
  for (const [s, ra] of a.m) {
    const rb = b.m.get(s); if (!rb) { real.push(s); continue; }
    if (ra.first === rb.first) { same++; continue; }
    (ra.cls === 'F' || rb.cls === 'F' ? budget : real).push(s);
  }
  if (b.m.size !== a.m.size) details.push(`fuzz seed counts differ: ${a.m.size} vs ${b.m.size}`);
  return { text: `${same}/${a.m.size} identical` + (budget.length ? `; budget-only ${budget.length} (${budget.join(' ')})` : '') + (real.length ? `; **real ${real.length}** (${real.join(' ')})` : '; real 0') + `; clean ${b.clean}`, real };
}

const R = [];
for (let i = 1; i <= N; i++) {
  const d = 'r' + i; const id = identity(d);
  R.push({ d, id, jit: jit(d, id), ms: mathsweep(d), suite: suite(d), chain: chain(d, 'regression.txt'), forced: chain(d, 'regression-forced.txt'),
    fr: fuzz(d, 'random'), fd: fuzz(d, 'defaults'), home: (rd(d, 'home.txt') || '').trim() });
}
const ref = R[0]; let realAny = 0;
const main = [], evidence = [];
main.push('| runtime | family | fluid suites: tests; printed lines vs r1 | chain-100 exact vs in-tree record | fuzz random: first holds vs r1 | fuzz defaults: first holds vs r1 | verdict |');
main.push('|---|---|---|---|---|---|---|');
evidence.push('| runtime | JIT during the suite run | Math != StrictMath on this runtime | Math results vs r1 (MathSweep hashes) | chain-100 with science code compiled before first call |');
evidence.push('|---|---|---|---|---|');
for (const r of R) {
  let real = 0, suiteText;
  const s = r.suite, rs = ref.suite;
  if (!s || !rs) { suiteText = 'not run'; real++; }
  else {
    let identical = 0, orderOnly = 0; const diff = [];
    const n = Math.max(s.printed.length, rs.printed.length);
    for (let k = 0; k < n; k++) {
      const a = rs.printed[k], b = s.printed[k];
      if (a === b) identical++; else if (a !== undefined && b !== undefined && normReasons(a) === normReasons(b)) orderOnly++; else diff.push(k + 1);
    }
    const failSame = JSON.stringify(s.failSet) === JSON.stringify(rs.failSet);
    const failed = (/failed=(\d+)/.exec(s.summary) || [, '?'])[1], found = (/found=(\d+)/.exec(s.summary) || [, '?'])[1];
    suiteText = `${found} tests, ${failed} failed${failSame ? (r === ref ? '' : ' (same tests)') : ' (**other tests**)'}; ${s.printed.length} lines: `
      + (diff.length ? `**${diff.length} differ** (lines ${diff.slice(0, 5).join(', ')}${diff.length > 5 ? ', ...' : ''})` : r === ref ? 'reference' : 'identical')
      + (r === ref ? '' : `, ${orderOnly} reason-order only`) + `; ${s.timing.length} with times masked, ${s.frames} JDK frames${s.jvmWarnings ? ` and ${s.jvmWarnings} JVM warning lines` : ""} left out`;
    details.push(`== ${r.d} lines with wall times (masked; a median table dropped): ${s.timing.map(l => l.slice(0, 120)).join(' | ')}`);
    if (diff.length || !failSame) {
      real++;
      details.push(`== ${r.d} printed lines differing from r1 (first 20): ${diff.slice(0, 20).join(' ')}`);
      for (const k of diff.slice(0, 20)) { details.push(`  < ${(rs.printed[k - 1] || '').slice(0, 400)}`); details.push(`  > ${(s.printed[k - 1] || '').slice(0, 400)}`); }
      if (!failSame) details.push(`  failures r1: ${rs.failSet.join(' | ')}\n  failures ${r.d}: ${s.failSet.join(' | ')}`);
    }
  }
  const same = sameChain(r.chain, ref.chain);
  const chainText = `${r.chain.short}; ${r === ref ? 'reference' : same ? 'identical to r1' : '**differs from r1**'}`;
  if (!same) { real++; details.push(`== ${r.d} chain-100: ${r.chain.dev} | ${r.chain.counts}\n   r1: ${ref.chain.dev} | ${ref.chain.counts}`); }
  const sameForced = sameChain(r.forced, ref.chain);
  if (!sameForced) { real++; details.push(`== ${r.d} chain-100 forced-compiled: ${r.forced.dev} | ${r.forced.counts}\n   r1 tiered: ${ref.chain.dev} | ${ref.chain.counts}`); }
  const cr = r === ref ? { text: `reference; clean ${r.fr ? r.fr.clean : '-'}`, real: [] } : fuzzCmp(ref.fr, r.fr);
  const cd = r === ref ? { text: `reference; clean ${r.fd ? r.fd.clean : '-'}`, real: [] } : fuzzCmp(ref.fd, r.fd);
  real += cr.real.length + cd.real.length;
  for (const [c, dist, a, b] of [[cr, 'random', ref.fr, r.fr], [cd, 'defaults', ref.fd, r.fd]])
    for (const sd of c.real) details.push(`== ${r.d} fuzz ${dist} seed ${sd}\n   r1: ${(a.m.get(sd) || { first: 'missing' }).first.slice(0, 300)}\n   ${r.d}: ${(b.m.get(sd) || { first: 'missing' }).first.slice(0, 300)}`);
  realAny += real;
  main.push(`| ${r.d}: ${r.id.name} | ${r.id.family} | ${suiteText} | ${chainText} | ${cr.text} | ${cd.text} | ${real ? '**REAL DIFFERENCE**' : r === ref ? 'reference' : 'identical (budget holds excepted)'} |`);
  const fns = Object.keys(r.ms);
  const libm = fns.filter(f => r.ms[f].differ > 0);
  const unstable = fns.filter(f => !r.ms[f].stable);
  const vsRef = fns.filter(f => !ref.ms[f] || ref.ms[f].hash !== r.ms[f].hash);
  evidence.push(`| ${r.d} | ${r.jit} | ${fns.length ? (libm.join(', ') || 'none') : 'not run'}${unstable.length ? ` (tier-dependent: ${unstable.join(', ')})` : ''} | ${r === ref ? 'reference' : fns.length ? (vsRef.length ? 'differ: ' + vsRef.join(', ') : 'all equal') : 'not run'} | ${r.forced.short}; ${sameForced ? 'identical to r1 tiered' : '**differs from r1**'} |`);
}
const table = [...main, '', ...evidence, '',
  `r1 chain-100 line: ${ref.chain.dev} (${ref.chain.counts}). Homes: ${R.map(r => r.d + '=' + r.home).join('; ')}.`].join('\n');
fs.writeFileSync(path.join(out, 'table.md'), table + '\n');
fs.writeFileSync(path.join(out, 'details.txt'), details.join('\n') + '\n');
console.log(table);
console.log(`\ndetails: ${path.join(out, 'details.txt')}`);
console.log(realAny ? `winsweep: REAL DIFFERENCES (${realAny})` : 'winsweep: all runtimes identical (budget holds excepted)');
process.exit(realAny ? 1 : 0);

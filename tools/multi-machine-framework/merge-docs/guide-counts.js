// Staged code guide: measured suite counts from the final lane at ce71b45c, and process-control's facts schema line.
// Each replacement must match exactly once, or the script aborts before writing anything.
const fs = require('fs'), path = require('path');
const dir = process.argv[2];
const lane = 'the final lane at `ce71b45c`: `test` 1643 tests in 288 classes, `slowGates` 188 in 18 classes, 0 failures; chain-100 exact; fluid GameTests 34/34; campaign as at 0.14.1 (cold random 975, bundled random 994, bundled defaults 1000)';
const edits = {
  'verification.md': [
    ['34 fluid GameTests in the source; suite counts not re-measured for this guide)',
     '34 fluid GameTests in the source; suite counts from ' + lane + ')'],
    ['Suite counts are not re-measured for this guide; the batch review records the final lane.',
     'Suite counts from ' + lane + ' (`MERGE_REVIEW.md`).'],
  ],
  'README.md': [
    ['346 test files; suite counts are not re-measured here, the batch review records the final lane.',
     '346 test files. Suite counts from ' + lane + '.'],
  ],
  'process-control.md': [
    ['· **Updated:** 2026-10-27 (batch 2026-10-26-process-control:',
     '· **Updated:** 2026-10-27 (batch 2026-10-26-multi-machine-framework: facts schema 4 named beside schema 3; the code of this chapter is unchanged) · earlier: 2026-10-27 (batch 2026-10-26-process-control:'],
    ['schemas 1 and 2 still read. State format 22 can rebuild.',
     'schemas 1 and 2 still read. Schema 4 (0.15.0) retires the basis hash and appends the basis revision ([fluid-runtime.md](fluid-runtime.md)). State format 22 can rebuild.'],
  ],
};
const out = {};
for (const [file, list] of Object.entries(edits)) {
  let text = fs.readFileSync(path.join(dir, file), 'utf8');
  for (const [from, to] of list) {
    const n = text.split(from).length - 1;
    if (n !== 1) { console.error(`ABORT ${file}: anchor found ${n} times: ${from.slice(0, 70)}`); process.exit(1); }
    text = text.replace(from, () => to);
  }
  out[file] = text;
}
// process-control's snapshot moves to the merge commit with its Updated line.
const pc = out['process-control.md'];
const snap = /\*\*Snapshot:\*\* 0e697177 \(main, mod 0\.14\.1\), 2026-10-27/;
if (!snap.test(pc)) { console.error('ABORT process-control.md: snapshot line'); process.exit(1); }
out['process-control.md'] = pc.replace(snap, '**Snapshot:** @@MERGE@@ (main, mod 0.15.0), 2026-10-27');
for (const [file, text] of Object.entries(out)) fs.writeFileSync(path.join(dir, file), text, 'utf8');
console.log('patched', Object.keys(out).join(', '));

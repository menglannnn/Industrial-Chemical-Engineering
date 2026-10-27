// Batch README: the final lane at ce71b45c, MERGE_REVIEW.md in the documents, option C in the gate review's entry.
// Each anchor must match exactly once, or nothing is written.
const fs = require('fs');
const file = process.argv[2];
let text = fs.readFileSync(file, 'utf8');
const edits = [
  ['W8 (merge preparation) in progress since 2026-10-27.',
   'W8 (merge preparation): final lane green at `ce71b45c` ([MERGE_REVIEW.md](MERGE_REVIEW.md)); the merge waits for the owner.'],
  ['options (A) rooms at the largest and (B) accept the touches.',
   'options (A) rooms at the largest and (B) accept the touches; the off-centre measurement and (C), the owner\'s choice, implemented in `67343274`.\n- [MERGE_REVIEW.md](MERGE_REVIEW.md): the merge to main as 0.15.0: the first lane\'s failure, the final lane at `ce71b45c` against 0.14.1\'s, formats, the code guide, the tool folder, open items.'],
  ['Gates: `ProcessUnitTest` 23/0, `PipeLineResizeTest` 8/0, fluid GameTests 34/34.\n',
   'Gates: `ProcessUnitTest` 23/0, `PipeLineResizeTest` 8/0, fluid GameTests 34/34.\n- 2026-10-27, final lane at `ce71b45c`, run alone: `test` 1643 and `slowGates` 188, 0 failures; 33 junction lines and chain-100 exact as at 0.14.1; fluid GameTests 34/34; campaign clean 975 / 994 / 1000 with the same seeds (cold 833 classed F under load, B when replayed alone, as the baseline). Printed lines move only by this batch\'s identities, compat images and assembly store lines. Code guide staged (13 files), tool folder `tools/multi-machine-framework/` with the lane\'s logs. [MERGE_REVIEW.md](MERGE_REVIEW.md).\n'],
];
for (const [from, to] of edits) {
  const n = text.split(from).length - 1;
  if (n !== 1) { console.error(`ABORT: anchor found ${n} times: ${from.slice(0, 70)}`); process.exit(1); }
  text = text.replace(from, () => to);
}
fs.writeFileSync(file, text, 'utf8');
console.log('patched', file);

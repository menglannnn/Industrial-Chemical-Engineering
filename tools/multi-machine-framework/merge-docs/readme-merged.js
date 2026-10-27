// Batch README at the merge: status, history line, open item. Each anchor must match once, or nothing is written.
const fs = require('fs');
const file = process.argv[2];
let text = fs.readFileSync(file, 'utf8');
const edits = [
  ['Status: Planned (2026-10-26), revised twice',
   'Status: Implemented 2026-10-27 (0.15.0, main fast-forwarded to `ce71b45c`, not pushed). Planned (2026-10-26), revised twice'],
  ['W8 (merge preparation): final lane green at `ce71b45c` ([MERGE_REVIEW.md](MERGE_REVIEW.md)); the merge waits for the owner.',
   'W8 (merge preparation): final lane green at `ce71b45c` ([MERGE_REVIEW.md](MERGE_REVIEW.md)); merged to main 2026-10-27 as 0.15.0.'],
  ['[MERGE_REVIEW.md](MERGE_REVIEW.md).\n\n## Open items',
   '[MERGE_REVIEW.md](MERGE_REVIEW.md).\n- 2026-10-27, merge (the owner\'s go-ahead): main fast-forwarded from 0.14.1 `0e697177` to `ce71b45c` as 0.15.0, not pushed. Code guide copied to the main checkout (11 chapters and the README updated, `machine-assemblies.md` new; snapshot `ce71b45c`), this folder copied, `documentation/INDEX.md` row Implemented; tool folder `tools/multi-machine-framework/` with its `tools/INDEX.md` row.\n\n## Open items'],
  ['- Merge of this batch to `main`: the owner\'s approval.',
   '- Push of `main` (0.14.1 and 0.15.0 are not pushed): the owner\'s approval.'],
];
for (const [from, to] of edits) {
  const n = text.split(from).length - 1;
  if (n !== 1) { console.error(`ABORT: anchor found ${n} times: ${from.slice(0, 70)}`); process.exit(1); }
  text = text.replace(from, () => to);
}
fs.writeFileSync(file, text, 'utf8');
console.log('patched', file);

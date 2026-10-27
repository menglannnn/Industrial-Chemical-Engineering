// Patches a shared file in the main checkout in place (memory: shared-index-patch-in-place):
//   node tmp/merge-docs/patch.js <spec.json>
// spec: { "file": "<absolute path>", "edits": [ { "old": "<exact text, LF>", "new": "<text, LF>" } ] }
// Each "old" must occur exactly once, else nothing is written. Line endings follow the file (CRLF kept).
const fs = require("fs");
const spec = JSON.parse(fs.readFileSync(process.argv[2], "utf8"));
let text = fs.readFileSync(spec.file, "utf8");
const crlf = text.includes("\r\n");
const eol = s => (crlf ? s.replace(/\r?\n/g, "\r\n") : s);
for (const edit of spec.edits) {
  const old = eol(edit.old);
  const count = text.split(old).length - 1;
  if (count !== 1) {
    console.error("ABORT: " + count + " matches for: " + edit.old.slice(0, 120));
    process.exit(1);
  }
  text = text.replace(old, () => eol(edit.new));
}
fs.writeFileSync(spec.file, text);
console.log("patched " + spec.file + " (" + spec.edits.length + " edits)");

// Replaces the text between two markers of a source file with a block from a file (offline edit helper of WP4).
// Usage: node splice.js <file> <block file> <start marker> <end marker>
// The start marker is replaced with the block; the end marker is kept.
const fs = require("fs");
const [file, blockFile, startMarker, endMarker] = process.argv.slice(2);
let s = fs.readFileSync(file, "utf8");
const start = s.indexOf(startMarker);
if (start < 0) throw new Error("start marker not found: " + startMarker);
const end = s.indexOf(endMarker, start);
if (end < 0) throw new Error("end marker not found: " + endMarker);
s = s.slice(0, start) + fs.readFileSync(blockFile, "utf8") + s.slice(end);
fs.writeFileSync(file, s);
console.log("spliced " + (end - start) + " characters");

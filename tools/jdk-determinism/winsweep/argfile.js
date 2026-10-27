// winsweep helper: reads build/fuzz-runner/classpath.args (java @argfile: "-cp" then one quoted, backslash-escaped path
// list), writes a copy with <dir> prepended to the class path, and prints the JUnit Platform jars of that class path
// (';'-separated) for compiling the MiniLauncher.   node argfile.js <classpath.args> <dir-to-prepend> <out-argfile>
const fs = require('fs');
const [src, dir, out] = process.argv.slice(2);
const lines = fs.readFileSync(src, 'utf8').replace(/\r/g, '').split('\n').filter(l => l.length);
if (lines[0] !== '-cp' || !/^".*"$/.test(lines[1])) throw new Error('unexpected classpath.args format: ' + src);
const cp = lines[1].slice(1, -1).replace(/\\(.)/g, '$1');
const q = s => '"' + s.replace(/\\/g, '\\\\').replace(/"/g, '\\"') + '"';
fs.writeFileSync(out, '-cp\n' + q(dir + ';' + cp) + '\n');
console.log(cp.split(';').filter(p => /[\\/](junit-platform-(launcher|engine|commons)|opentest4j|apiguardian-api)-[^\\/]*\.jar$/.test(p)).join(';'));

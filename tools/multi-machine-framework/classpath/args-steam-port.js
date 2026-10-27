// Writes javac argument files (forward slashes: an argument file reads a backslash as an escape) for compile checks
// into tmp/steam-port/: javac-main.args (main sources into main-classes) and javac-test.args (tests against them).
const fs = require('fs');
const cp = fs.readFileSync('tmp/line-resize/cp.txt', 'utf8').split(';').filter(e => e !== 'build/classes/java/test').map(e => e.split('\\').join('/'));
const write = (name, entries, out) => fs.writeFileSync('tmp/steam-port/' + name,
    ['-nowarn', '-proc:none', '-encoding', 'UTF-8', '-cp', '"' + entries.join(';') + '"', '-d', out].join('\n') + '\n');
write('javac-main.args', cp, 'tmp/steam-port/main-classes');
write('javac-test.args', ['tmp/steam-port/main-classes'].concat(cp), 'tmp/steam-port/test-classes');
console.log('entries ' + cp.length);

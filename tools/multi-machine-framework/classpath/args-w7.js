// Writes tmp/w7/cp.args (javac) and tmp/w7/run.args (java) from tmp/line-resize/cp.txt, with forward slashes.
const fs = require('fs');
const cp = fs.readFileSync('tmp/line-resize/cp.txt', 'utf8').split(String.fromCharCode(92)).join('/');
fs.writeFileSync('tmp/w7/cp.args', '-cp\n"' + cp + '"\n');
fs.writeFileSync('tmp/w7/run.args', '-cp\n"tmp/w7/classes;' + cp + '"\n');
console.log('ok');

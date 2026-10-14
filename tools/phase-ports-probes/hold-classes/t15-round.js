// node round.js <file> <digits>: prints lines with every float rounded to <digits> significant digits
const fs=require('fs');const d=+process.argv[3];
for(const l of fs.readFileSync(process.argv[2],'utf8').split(/\r?\n/))console.log(l.replace(/-?\d+\.\d+(?:[eE][-+]?\d+)?|-?\d+[eE][-+]?\d+/g,m=>{const v=+m;return v===0?'0':v.toPrecision(d)}));

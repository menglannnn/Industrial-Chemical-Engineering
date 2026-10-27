const fs=require('fs');const f=process.argv[2];let s=fs.readFileSync(f,'utf8');
const before=`            try{x=solve(closing,start);}
            catch(SparseNewton.Nonconvergence failure){`;
const i=s.indexOf(before);if(i<0)throw new Error('no try');
const end=s.indexOf(`                throw failure;
            }
`,i);if(end<0)throw new Error('no end');
s=s.slice(0,i)+`            var x=solve(closing,start);
`+s.slice(end+`                throw failure;
            }
`.length);
s=s.replace(`            var closing=joint(start);double[] x;
            var x=solve(closing,start);`,`            var closing=joint(start);var x=solve(closing,start);`);
if(s.includes('PROBE'))throw new Error('probe left');
fs.writeFileSync(f,s);

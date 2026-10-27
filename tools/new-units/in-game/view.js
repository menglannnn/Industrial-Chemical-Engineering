// Write a spectator view file: node view.js <file> <tx> <ty> <tz> <dist> <yaw> <pitch>
// The eye looks at (tx, ty, tz) from dist blocks; tp y is the eye minus 1.62.
const [file, tx, ty, tz, d, yaw, pitch] = process.argv.slice(2);
const r = Math.PI / 180, Y = +yaw * r, P = +pitch * r, D = +d;
const lx = -Math.sin(Y) * Math.cos(P), ly = -Math.sin(P), lz = Math.cos(Y) * Math.cos(P);
const ex = +tx - D * lx, ey = +ty - D * ly, ez = +tz - D * lz;
require('fs').writeFileSync(file, `gamemode spectator\ntp @s ${ex.toFixed(2)} ${(ey - 1.62).toFixed(2)} ${ez.toFixed(2)} ${(+yaw).toFixed(1)} ${(+pitch).toFixed(1)}\n`);
console.log(file, ex.toFixed(1), ey.toFixed(1), ez.toFixed(1));

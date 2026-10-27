// Minimal NBT (gzip, big-endian) reader and writer for level.dat.
//   node nbt.js dump <level.dat> [path.in.data]
//   node nbt.js review <level.dat> <name>   -> LevelName, no experimental features or packs (writes level.dat in place)
const fs = require('fs');
const zlib = require('zlib');

function read(buf) {
    let o = 0;
    const u8 = () => buf.readUInt8(o++), i16 = () => { const v = buf.readInt16BE(o); o += 2; return v; },
        i32 = () => { const v = buf.readInt32BE(o); o += 4; return v; }, i64 = () => { const v = buf.readBigInt64BE(o); o += 8; return v; },
        f32 = () => { const v = buf.readFloatBE(o); o += 4; return v; }, f64 = () => { const v = buf.readDoubleBE(o); o += 8; return v; },
        str = () => { const n = buf.readUInt16BE(o); o += 2; const s = buf.toString('utf8', o, o + n); o += n; return s; };
    function payload(t) {
        switch (t) {
            case 1: return buf.readInt8(o++);
            case 2: return i16();
            case 3: return i32();
            case 4: return i64();
            case 5: return f32();
            case 6: return f64();
            case 7: { const n = i32(); const a = Array.from(buf.subarray(o, o + n)).map(b => (b << 24) >> 24); o += n; return a; }
            case 8: return str();
            case 9: { const et = u8(), n = i32(); const items = []; for (let k = 0; k < n; k++) items.push(payload(et)); return { et, items }; }
            case 10: { const m = []; for (;;) { const tt = u8(); if (tt === 0) break; const name = str(); m.push([name, tt, payload(tt)]); } return m; }
            case 11: { const n = i32(); const a = []; for (let k = 0; k < n; k++) a.push(i32()); return a; }
            case 12: { const n = i32(); const a = []; for (let k = 0; k < n; k++) a.push(i64()); return a; }
        }
        throw new Error('tag ' + t);
    }
    const t = u8(), name = str();
    return { t, name, value: payload(t) };
}

function write(root) {
    const parts = [];
    const b = (n, f) => { const x = Buffer.alloc(n); f(x); parts.push(x); };
    const s = v => { const x = Buffer.from(v, 'utf8'); b(2, y => y.writeUInt16BE(x.length)); parts.push(x); };
    function payload(t, v) {
        switch (t) {
            case 1: return b(1, x => x.writeInt8(v));
            case 2: return b(2, x => x.writeInt16BE(v));
            case 3: return b(4, x => x.writeInt32BE(v));
            case 4: return b(8, x => x.writeBigInt64BE(v));
            case 5: return b(4, x => x.writeFloatBE(v));
            case 6: return b(8, x => x.writeDoubleBE(v));
            case 7: b(4, x => x.writeInt32BE(v.length)); return b(v.length, x => v.forEach((e, k) => x.writeInt8(e, k)));
            case 8: return s(v);
            case 9: b(1, x => x.writeUInt8(v.items.length ? v.et : (v.et || 0))); b(4, x => x.writeInt32BE(v.items.length)); return v.items.forEach(e => payload(v.et, e));
            case 10: for (const [name, tt, val] of v) { b(1, x => x.writeUInt8(tt)); s(name); payload(tt, val); } return b(1, x => x.writeUInt8(0));
            case 11: b(4, x => x.writeInt32BE(v.length)); return v.forEach(e => b(4, x => x.writeInt32BE(e)));
            case 12: b(4, x => x.writeInt32BE(v.length)); return v.forEach(e => b(8, x => x.writeBigInt64BE(e)));
        }
        throw new Error('tag ' + t);
    }
    b(1, x => x.writeUInt8(root.t)); s(root.name); payload(root.t, root.value);
    return Buffer.concat(parts);
}

const get = (compound, name) => { const e = compound.find(x => x[0] === name); return e ? e[2] : undefined; };
const show = (t, v, indent) => {
    if (t === 10) return '{\n' + v.map(([n, tt, val]) => indent + '  ' + n + ': ' + show(tt, val, indent + '  ')).join('\n') + '\n' + indent + '}';
    if (t === 9) return '[' + v.items.map(e => show(v.et, e, indent + '  ')).join(', ') + ']';
    if (Array.isArray(v)) return '[' + v.length + ' values]';
    return typeof v === 'bigint' ? v.toString() + 'L' : JSON.stringify(v);
};

const [cmd, file, arg] = process.argv.slice(2);
const root = read(zlib.gunzipSync(fs.readFileSync(file)));
const data = get(root.value, 'Data');
if (cmd === 'dump') {
    let v = data, t = 10;
    if (arg) for (const part of arg.split('.')) { const e = v.find(x => x[0] === part); t = e[1]; v = e[2]; }
    console.log(show(t, v, ''));
} else if (cmd === 'review') {
    const at = data.findIndex(x => x[0] === 'LevelName');
    data[at][2] = arg;
    // Experimental features and packs off: the world reads as a plain one.
    const features = get(data, 'enabled_features');
    if (features) features.items = features.items.filter(f => f === 'minecraft:vanilla');
    const packs = get(data, 'DataPacks');
    if (packs) {
        const enabled = get(packs, 'Enabled'), disabled = get(packs, 'Disabled');
        const experimental = ['bundle', 'trade_rebalance', 'redstone_experiments', 'minecart_improvements', 'winter_drop', 'update_1_21'];
        const moved = enabled.items.filter(p => experimental.includes(p));
        enabled.items = enabled.items.filter(p => !experimental.includes(p));
        if (disabled && moved.length) {
            disabled.et = 8;
            disabled.items.push(...moved);
        }
    }
    fs.writeFileSync(file, zlib.gzipSync(write(root)));
    console.log('written ' + file);
}

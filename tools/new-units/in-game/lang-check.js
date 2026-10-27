// Static check: each "<kind>.<name>" literal in the new unit designs has a constructor key (and .hint) or a vessel key.
const fs = require('fs'), path = require('path');
const lang = JSON.parse(fs.readFileSync('src/main/resources/assets/createcheme/lang/en_us.json', 'utf8'));
const kinds = ['stirred_reactor','adsorber','trickle_bed_reactor','ebullated_bed_reactor','centrifugal_compressor','steam_turbine','condensing_turbine','cyclone','fcc_converter','delayed_coker','fluidized_bed_boiler','fluidized_bed_gasifier','grate_boiler','fixed_bed_gasifier'];
const dir = 'src/main/java/com/wormzjl/createcheme/science/equipment';
const C = 'gui.createcheme.equipment_constructor.', V = 'gui.createcheme.vessel.';
let missing = [];
for (const k of kinds) {
  for (const key of [C + 'equipment.' + k, C + 'equipment.' + k + '.hint', C + 'equipment.' + k + '.short', V + 'title.' + k, V + 'unit_support.' + k])
    if (!(key in lang)) missing.push(key);
  if (!Object.keys(lang).some(x => x.startsWith(V + 'kind.' + k))) missing.push(V + 'kind.' + k + '*');
}
for (const f of fs.readdirSync(dir).filter(f => f.endsWith('.java'))) {
  const src = fs.readFileSync(path.join(dir, f), 'utf8');
  for (const m of src.matchAll(/"([a-z_]+)\.([a-z_.]+)"/g)) {
    if (!kinds.includes(m[1])) continue;
    const s = m[1] + '.' + m[2];
    if (!(C + s in lang) && !(V + s in lang)) missing.push(f + ': ' + s);
    if (C + s in lang && !(C + s + '.hint' in lang)) missing.push(f + ': ' + s + '.hint');
  }
}
console.log(missing.length ? missing.join('\n') : 'all keys present');

// WP4: the fluid_domain evidence texts that described the old hold rule (a state valid only inside every component's
// range; ice not modelled) say what the network does now: the data range, continued past, and water's ice.
const fs=require('fs');const root=process.argv[2];
const carried=' A state past the range of a component it carries is carried, its properties continued and the island\'s status line noting it (owner rule of 2026-10-16); free water below its melting line freezes as ice Ih (WP4).';
function edit(path,from,to){const f=root+'/'+path;let s=fs.readFileSync(f,'utf8');if(!s.includes(from))throw new Error('missing in '+path+': '+from);s=s.replace(from,to);fs.writeFileSync(f,s);}
const family=['bonga_tjl20','cold_lake_blend_tjl20','dalia_tjl20','tjl20','upper_zakum_tjl20','wti_light_export_tjl20'];
for(const p of family)edit('packages/'+p+'.json',
  'Fluid network envelope: from the water model triple point (273.16 K; ice is not modelled), the lowest minimum of any component of this package, to the 900 K of the caloric fits; 100 Pa..10 MPa holds the methane record\'s declared range (P3 WP7); every other component keeps its own 2 MPa, and a state is valid only inside the range of every component it carries. Every phase at its own pressure (P3 WP4).',
  'Fluid network envelope, the package\'s data range: from the water model triple point (273.16 K), the lowest minimum of any component of this package, to the 900 K of the caloric fits; 100 Pa..10 MPa holds the methane record\'s declared range (P3 WP7); every other component keeps its own 2 MPa.'+carried+' Every phase at its own pressure (P3 WP4).');
edit('packages/tjl19.json',
  'Fluid network envelope: from the water model triple point (273.16 K; ice is not modelled), the lowest minimum of any component of this package, to the 900 K of the caloric fits; 100 Pa..2 MPa is the span the fluid network evaluates, every phase at its own pressure (P3 WP4).',
  'Fluid network envelope, the package\'s data range: from the water model triple point (273.16 K), the lowest minimum of any component of this package, to the 900 K of the caloric fits; 100 Pa..2 MPa is the span of its data, every phase at its own pressure (P3 WP4).'+carried);
edit('packages/tjl20_nitrogen.json',
  'the crude components, ethane and the light ends keep their own 2 MPa, and a state is valid only inside the range of every component it carries. Every phase at its own pressure (P3 WP4). The crude components keep 293.15 K and water its 273.16 K triple point inside it.',
  'the crude components, ethane and the light ends keep their own 2 MPa.'+carried+' Every phase at its own pressure (P3 WP4). The crude components keep 293.15 K and water its 273.16 K triple point inside it.');
edit('packages/pilot_cryogenic.json',
  'every component\'s own range lies inside, and a state is valid only inside the range of every component it carries. Qualified at G3 (WP9); the declared critical band is research-only. Under the crystal competition (G4, G5; D17 to D19) a gas and a methane-rich liquid carry CO2 below its triple point to 90 K.',
  'every component\'s own range lies inside.'+carried.replace(' (WP4).',', beside CO2-I (no hydrates, WP4).')+' Qualified at G3 (WP9); the declared critical band is research-only. Under the crystal competition (G4, G5; D17 to D19) a gas and a methane-rich liquid carry CO2 below its triple point, qualified to 90 K.');
edit('properties/pilot_carbon_dioxide.json',
  'Below its triple point CO2 is carried to 90 K only under the package\'s crystal competition (P4, P5; D17 to D19: a gas, and a methane-rich liquid beside or towards its CO2-I crystal); this record\'s fluid-only range stays 216.592 K and up.',
  'Below its triple point CO2 freezes under the package\'s crystal competition (P4, P5; D17 to D19: a gas, and a methane-rich liquid beside or towards its CO2-I crystal), qualified to 90 K; this record\'s data range is 216.592 K and up, and past it the fluid is carried with its properties continued (owner rule of 2026-10-16), a triple point being no lower bound.');

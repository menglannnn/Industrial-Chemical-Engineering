// WP4: the scientific revisions move with the water record (its ice and revision r2); the physics fingerprints do not.
const fs=require('fs');const f=process.argv[2];let s=fs.readFileSync(f,'utf8');
function swap(from,to){if(!s.includes(from))throw new Error('missing: '+from.slice(0,90));s=s.split(from).join(to);}
const moved={
 'crude-regrouped-r1:61dcf6df94ee836e4063f2871c761e3fad61576e8a2c62e80cb4f2b3ab5528f3':'crude-regrouped-r1:2b656d117d1b18f4870ba7eba0552434ba2461dd9f8f39834c8ca0ad511f3030',
 'crude-regrouped-r1:08620e3b0bb8b059d3a54bb80ac904faa89ece1fb1c405339eb14012f3a7d7e3':'crude-regrouped-r1:2d8062c949b9f081dc0f49373701b3f23eeb46b5b5765a0b1e08f2e0cbbbb5b9',
 'crude-regrouped-r1:36ef9ad1fea2c55ea7a8bc5a618a84358e4cdb669bc1b93395ff0293eae119fd':'crude-regrouped-r1:7fb5cd2a052f6b94f7673f63a9b4d702c7b2c3cfd2793da8ea667965e24bb811',
 'crude-regrouped-r1:f74f65646c865ee96e22c89ee13a37dfa81e9a4aa918c4e4df218c64df0b6a18':'crude-regrouped-r1:49a39c7185be6213b57c9ef520074f5afacc227e94cd8795acb319db01900948',
 'crude-regrouped-r1:dc4be6bfc41142eeb374e0fa2cad11120258db1d0bd99fe770c8fce607b071e4':'crude-regrouped-r1:aa4d0b8ad847942cb4f3654fd372adac5ff1561057420a30e3ee6a13a555d4e5',
 'crude-regrouped-r1:c8e890f912d4c4626e8c14fa41d267c8704480d7305ad3ccb2d02f47caf9a341':'crude-regrouped-r1:a390e042a693d7388a0ca305ee0dbe1546cf1097c2ef97f947bd65ee91c50006',
 'crude-regrouped-r1:8274de881b2c1aac526ec81f9c8d8142f76a3fca956dcfdd0f19a7b132bc45b2':'crude-regrouped-r1:8ee3e5d38c69163987197a14dd050356d4a5aac420660368ae1a4a372f279b85',
 'crude-regrouped-r1:9f0540a5aecea7f54f60a61c2eb630dec95ee5796a00b037dddafd705f29eacc':'crude-regrouped-r1:e6dfc5112227020090411e6e5413549f8d53b54a146ba68c90b7884e2c326aec',
 'pilot-cryogenic-p3-r1:37a33a6190bd9fba7433c494c63ae4bfab24995b6902cf58c59381033c3a7c64':'pilot-cryogenic-p3-r1:c83264ecee95d1c445ea1e73854500e08a202bac63bae0050dab9e6b58bc82f1'};
for(const [a,b] of Object.entries(moved))swap(a,b);
swap(`    /**
     * Package id, scientific revision and physics fingerprint of every bundled column and network package, read at
     * 9ca0f9c before the P2 loader change (a javac-run main against the compiled classes) and unchanged since.
     */`,`    /**
     * Package id, scientific revision and physics fingerprint of every bundled column and network package, read at
     * 9ca0f9c before the P2 loader change (a javac-run main against the compiled classes). The physics fingerprints are
     * unchanged since; the scientific revisions moved once, with the water record every package shares (WP4 of the
     * range-extrapolation plan: its ice Ih and revision {@code water-iapws-shomate-r2}; bonga's was {@code 61dcf6df...}).
     */`);
swap(`enthalpy of 9019 J/mol. The scientific revision and the physics fingerprint do not see the spine or the crystal and
     * did not move.</p>`,`enthalpy of 9019 J/mol. The scientific revision and the physics fingerprint do not see the spine or the crystal and
     * did not move. WP4 of the range-extrapolation plan moved the scientific revision from {@code 37a33a61...}, with the
     * water record's ice (as every package's), not the physics fingerprint.</p>`);
swap(`            assertEquals(1, catalog.crystals().size());`,`            // CO2-I, and the free water's ice Ih (WP4), which every package's water record names.
            assertEquals(2, catalog.crystals().size());`);
fs.writeFileSync(f,s);

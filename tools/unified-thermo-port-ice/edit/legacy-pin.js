// WP4: LegacyNetworkPathPinTest re-pinned: the revision carries the free water's formulation and revision, the digest
// moves at the 27 wet flashes below 273.16 K only (the ice hold before).
const fs=require('fs');const f=process.argv[2];let s=fs.readFileSync(f,'utf8');
function swap(from,to){if(!s.includes(from))throw new Error('missing: '+from.slice(0,90));s=s.replace(from,to);}
swap(`     * model's triple point is the ice hold instead of a range refusal; and the 450 K, 2 MPa wet mixture past the steam's
     * qualified partial pressure is carried.</p>
     */
    static final String DIGEST = "1e75c5c55a42a8d064eadee7a59d9130b49033886b3945e589bc83dffa27ee8f";`,
`     * model's triple point is the ice hold instead of a range refusal; and the 450 K, 2 MPa wet mixture past the steam's
     * qualified partial pressure is carried.</p>
     *
     * <p>Re-pinned for the free water's ice (WP4 of the range-extrapolation plan; the value before,
     * {@code 1e75c5c55a42a8d064eadee7a59d9130b49033886b3945e589bc83dffa27ee8f}). Point by point
     * ({@code tools/unified-thermo-port-extrapolation/probes/LegacyPinPointProbe.java} at both trees) only the 27 wet
     * flashes at 77, 110 and 150 K moved, each the ice hold before: 26 are answered, the water ice beside the
     * hydrocarbons, and the twelve-component mixture at 110 K and 1 bar is the three-phase hold its hydrocarbons meet dry.</p>
     */
    static final String DIGEST = "6e9fbe83094f3a9fa9aeee3460f89f22b0e74931e035e3adca39c9f196e0b7e4";`);
swap(`     * data) replace main 0.8.0's: a fresh world.
     */
    static final String REVISION = "95cd8e6d4b4d4af2dfb8e712e443eda42acb0c93b4218635e7ebf462e21829cd:direct-liquid-v1:tp-engine-v1:range-continuation-v1"
            + ":fluid-domain-data-v1:cp-segments-v1:catalog-volume-reference-v1";`,
`     * data) replace main 0.8.0's: a fresh world. Since WP4 it carries the free water's formulation and revision
     * ({@code separate-free-water-v2:water-iapws-shomate-r2}: its ice), as a spine package's identity does, and the
     * fingerprint hashes the ice's science.
     */
    static final String REVISION = "0b64e8814c6a995993f80d1446d88bd2e1a258e664552d1d1128976fcd004352:direct-liquid-v1:tp-engine-v1:range-continuation-v1"
            + ":separate-free-water-v2:water-iapws-shomate-r2:fluid-domain-data-v1:cp-segments-v1:catalog-volume-reference-v1";`);
fs.writeFileSync(f,s);

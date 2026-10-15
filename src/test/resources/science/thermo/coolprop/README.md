# CoolProp fluid files and parity fixtures (test resources)

Test-only data for the Java Helmholtz reference oracle in `src/test/java/com/wormzjl/createcheme/science/thermo/reference`
(batch `2026-09-24-coolprop-low-temperature`, plan P1 item 1, decision D4). Nothing here enters the mod jar.

## Fluid files

Fetched unmodified from the CoolProp repository at revision `ae81610e7d23efc57f9d051c8e70a4d66e87537f`
(the revision CoolProp 8.0.0 reports as `CoolProp.__gitrevision__`):

    https://raw.githubusercontent.com/CoolProp/CoolProp/ae81610e7d23efc57f9d051c8e70a4d66e87537f/dev/fluids/<Name>.json
    https://raw.githubusercontent.com/CoolProp/CoolProp/ae81610e7d23efc57f9d051c8e70a4d66e87537f/LICENSE

| File | SHA-256 of the fetched bytes |
| --- | --- |
| `Nitrogen.json` | `791432d1685eafcc75b43e776e8fe4e30f7cf73a2a73b67de0c1a94c57a2b9aa` |
| `CarbonDioxide.json` | `08e27e1a5a6029e508976496b9d1fd4f7f0caa2c46fd35c84b7bf520260a5217` |
| `Methane.json` | `534b6a8906be63dd491972ddb29f2856c565c2c209320044858e67b90cb7a171` |
| `Ethane.json` | `ad4199d0665196aa99203515feb33db04c3ce6a3271c9536de6efb13a4f8803e` |
| `Hydrogen.json` | `515ef2140273fe8f8012e202158af11b5710c02c6af1caac2193669447b1ca25` |
| `Water.json` | `85021d3803d2b02edb1d3e90bd60f4eee4a70380e06ed6b7b44e57716b4c2266` |
| `LICENSE` | `9bf835333ef602af4cb19338b9f9d43671e174fa029b00280e9bdba6ea4719b2` |

Licence: MIT, copyright Ian H. Bell and other CoolProp developers; the full text is in `LICENSE` beside this file.
`.gitattributes` stores this directory as binary (`-text`) so the hashes hold on every checkout.

What the oracle reads from each file (first `EOS` block only):

- `EOS[0].alpha0`: IdealGasHelmholtzLead, LogTau, Power, PlanckEinstein, PlanckEinsteinFunctionT, EnthalpyEntropyOffset.
- `EOS[0].alphar`: ResidualHelmholtzPower (including the exp(-delta^l) terms), Gaussian, NonAnalytic (water and CO2).
- `EOS[0].gas_constant`, `molar_mass`, `Ttriple`, `T_max`, `p_max`, `STATES.reducing` (T, rhomolar).
- `STATES.critical` (T, p, rhomolar): root selection of the density solve.
- `ANCILLARIES.pS`, `rhoL`, `rhoV`: starting values only.

Not used: `TRANSPORT`, `SUPERANCILLARY`, `critical_region_splines`, the melting line and the other ancillaries, `INFO`
beyond the name. An unknown term type is refused at load, so a new fluid file with, for example, ammonia's
`ResidualHelmholtzGaoB` term fails loudly instead of evaluating a truncated equation.

## Parity fixtures

`parity-<Name>.json` are generated with CoolProp 8.0.0 (HEOS backend, default configuration) by
`tools/coolprop-parity/generate_parity.py` in the main checkout (git-ignored; its README gives the exact command).
Each holds single-phase states on a (T, P) grid plus the states of the review's PR78-vs-CoolProp probe, with CoolProp's
PT-flash outputs and CoolProp's EOS values at the density it reported, and saturation states from Ttriple to 0.99 Tc.
`HelmholtzReferenceParityTest` documents what is compared and at which tolerance.

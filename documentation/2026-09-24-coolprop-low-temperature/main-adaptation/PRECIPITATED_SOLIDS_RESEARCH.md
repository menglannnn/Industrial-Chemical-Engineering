# Precipitated solids: particle size and mobility (research note)

Date: 2026-10-15. This is research input for the lead's proposal. No code was read or changed. The scope was cut on owner feedback ("lightweight and works"): five sources for (a) to (d), round numbers, one threshold per rule. Section (e), added after a follow-up, uses two more sources. Confidence: H = high, M = medium, L = low.

## (a) Which liquid phase is continuous

- Rule: the liquid phase that holds more than half of the liquid volume is continuous. The other phase is dispersed as droplets. For an oil of about 1 mPa·s, inversion occurs at a water fraction of about 0.5 [1] (M).
- Refinement: φ_w,inv ≈ 0.5 − 0.11·log10(μ_oil / 1 mPa·s). Viscous oils therefore invert at lower water fractions: about 0.39 at 10 mPa·s and about 0.28 at 100 mPa·s [1]. Confidence M: the coefficient comes from a later paper that quotes the correlation, and the original was not read.
- Consequence: when the dispersed phase freezes, it forms separate particles. When the continuous phase freezes, it forms a solid matrix (a monolith) with the other liquid trapped inside.

## (b) Primary particle size per formation route

| Route | Default | Basis | Conf. |
|---|---|---|---|
| Freezing of the dispersed phase (each droplet freezes in place) | 0.1 mm; in pipes, see (e) | Each particle is the size of its parent droplet. Hinze [2] gives the largest stable droplet as d_max = 0.725 (σ/ρ_c)^0.6 ε^-0.4. Own estimate for σ = 0.03 N/m: d_max ≈ 0.5 mm near a stirred-tank impeller (ε ≈ 20 W/kg), and 1 to 4 mm in a 0.1 m pipe at 3 to 1 m/s. The mean droplet is about half of d_max. | M |
| Precipitation from solution (crystals grow in a liquid; this includes a continuous phase that freezes gradually) | 0.1 mm | MSMPR mass-dominant size L_D = 3Gτ [3]. With G ≈ 1e-8 m/s and τ ≈ 1 h, L_D ≈ 0.11 mm. With G = 1e-8 to 1e-7 m/s and τ = 0.5 to 1 h, the range is 0.05 to 1 mm. | H for the formula, L for the typical G |
| Desublimation from gas (CO2 snow) | 0.001 mm | Dry ice from expanding liquid CO2 forms primary particles of about 1 µm mass-median size. These deposit on the tube walls and come off as agglomerates of about 0.1 mm, which are smaller at higher flow velocity [4]. | M |

- The phase ratio decides the geometry (particles or monolith), not the particle size. Keep one size per route. Both liquid routes then share a single 0.1 mm default.

## (c) Solid fraction at which a slurry stops and blocks an outlet

- Rule: an outlet blocks when solids make up 0.5 of the slurry volume, counting liquid plus solid and excluding vapour (M).
- Basis: in the Krieger-Dougherty and Mills relations, viscosity rises only modestly up to 40 to 50 % solids. It diverges at the maximum packing fraction, usually taken as 0.64 (sometimes 0.74) [5]. The mod uses Krieger-Dougherty with φ_max = 0.62 and [η] = 2.5. With those values, the relative viscosity is about 13 at 0.5 and about 200 at 0.6. A cut-off at 0.5 keeps the steep part of the curve out of the solver.
- The threshold agrees with rule (a). When a major phase (above 0.5) freezes, solids exceed 0.5 and the outlet blocks. When a minority phase freezes, it stays a flowing slurry. One number covers the owner's intent.
- Refinement: cohesive solids, such as ice or hydrate in oil without anti-agglomerant, aggregate and develop a yield stress once about 0.25 of the volume is converted water [5]. An optional "sticky" flag could lower the threshold to 0.25 to 0.3 for such solids (M).
- Not covered: plate-like crystals such as paraffin wax gel at a few wt %. A volume-fraction rule does not capture this (no source here; L).

## (d) Is "a pipe blocks when a solid forms in it" fair?

- It is fair as a conservative game rule (M). It matches the physics in two cases:
  - gas lines, where desublimated CO2 deposits on the tube wall [4];
  - a continuous phase that freezes.
- It is pessimistic for a dispersed minority solid in liquid flow. Such a solid flows as a slurry whose viscosity rises with the solid fraction. Hydrate-slurry transport studies rest on exactly this [5].
- Refinement: in liquid pipes, apply rule (c): carry the solid below 0.5 and block above it. Keep "a solid forms, so the pipe blocks" for gas lines.

## (e) Droplet size as a function of the dispersed fraction (follow-up, 2026-10-15)

Rule: a frozen particle is the size of the droplet it froze from. In pipe flow, that droplet grows with the dispersed fraction φ up to the inversion point of rule (a).

- Correlation (Brauner 2001 [6]). It combines the dilute and the dense form, and the larger term wins. SI units:
  `d_max = D · We^-0.6 · f^-0.4 · [ρ_m / (ρ_c (1 − φ))]^-0.4 · max(0.55, 2.22 · C_H^0.6 · (φ / (1 − φ))^0.6)`
  - D is the pipe inner diameter (m). U_m is the mixture velocity (m/s): total volume flow divided by pipe area. In no-slip dispersed flow it equals the continuous-phase velocity.
  - We = ρ_c U_m² D / σ, where ρ_c is the continuous-phase density (kg/m³) and σ the interfacial tension (N/m). C_H ≈ 1.
  - f = 0.046 Re^-0.2 is the Fanning friction factor for a smooth pipe, with Re = ρ_c U_m D / μ_c and μ_c in Pa·s. Then f^-0.4 = 3.43 Re^0.08, which yields Brauner's published constants 1.88 and 7.61.
  - ρ_m = φ ρ_d + (1 − φ) ρ_c. The density factor is close to 1 (0.72 at φ = 0.5) and can be dropped.
  - The 0.55 term is Hinze's dilute limit, with ε = 2 f U_m³ / D.
  - The 2.22 term is Brauner's dense-dispersion energy balance: the turbulence of the continuous phase must supply the surface energy of the drops. This term takes over above φ ≈ 0.09.
  - Particle size: use the Sauter mean, d32 ≈ 0.48 d_max [7]. For oil drops in water, the same formula applies with water as the continuous phase (ρ_c and μ_c of water).
- Worked case (own arithmetic): water in a light oil (ρ_c = 800 kg/m³, μ_c = 2 mPa·s, ρ_d = 1000 kg/m³, σ = 0.03 N/m), D = 0.05 m, U_m = 1 m/s. This gives We ≈ 1300, Re ≈ 20 000 and f ≈ 0.0063.

| φ | d_max | d32 = particle size |
|---|---|---|
| 0.05 | 3 mm (dilute term) | 1.3 mm |
| 0.2 | 4.5 mm | 2 mm |
| 0.4 | 7 mm | 3.3 mm |

- The size grows about 2.5 times from φ = 0.05 to φ = 0.4. It scales roughly as U_m^-1.1, so at 2 m/s it is about half these values. In pipes, these sizes replace the 0.1 mm default of (b); 0.1 mm is the scale of a strongly agitated vessel.
- Settling, from the same source [6]: a drop or particle larger than d_cb = (3/8) ρ_c f U_m² / (Δρ g) is not held up by turbulence and settles out. In the worked case, d_cb ≈ 1 mm for water drops (Δρ = 200 kg/m³) and ≈ 1.7 mm for ice (Δρ ≈ 120 kg/m³). Particle size, density difference, pipe and fluid properties all enter through f, U_m and Δρ. Confidence M: the form is recalled from memory and agrees with a simple force balance.
- Still tank (no flow, so no turbulence measure): with no breakup scale, drops coalesce and settle. Keep the droplet size the liquid arrived with, computed with the inlet pipe's D and U_m (1 m/s if unknown). Refinement: let a still tank stratify.
- Confidence M. The derivation above reproduces both constants exactly (0.55 = 0.725 · 2^-0.4 and 2.22 = 6^0.6 · 2^-0.4), and citing papers quote "2.22 C_H^0.6 We^-0.6". I did not read the paper itself, because its open copies are PDFs, which the brief excluded. One uncertainty remains: if Brauner's We uses the continuous-phase superficial velocity instead of U_m, the sizes increase by a factor (1 − φ)^-1.2, which is 1.85 at φ = 0.4.

## Sources

1. Arirachakaran, S. et al. (1989). An analysis of oil/water flow phenomena in horizontal pipes. SPE Production Operations Symposium, paper SPE 18836. https://doi.org/10.2118/18836-MS (correlation taken from later work that quotes it; original not read).
2. Hinze, J.O. (1955). Fundamentals of the hydrodynamic mechanism of splitting in dispersion processes. AIChE Journal 1(3), 289-295. https://doi.org/10.1002/aic.690010303
3. Randolph, A.D., Larson, M.A. (1988). Theory of Particulate Processes, 2nd ed. Academic Press. Source of the MSMPR dominant size L_D = 3Gτ.
4. Liu, Y.-H., Maruyama, H., Matsusaka, S. (2010). Agglomeration process of dry ice particles produced by expanding liquid carbon dioxide. Advanced Powder Technology 21(6), 652-657. https://doi.org/10.1016/j.apt.2010.07.009 (open copy: https://repository.kulib.kyoto-u.ac.jp/dspace/handle/2433/194294)
5. Majid, A.A.A., Wu, D.T., Koh, C.A. (2018). A perspective on rheological studies of gas hydrate slurry properties. Engineering 4(3), 321-329. https://doi.org/10.1016/j.eng.2018.05.017
6. Brauner, N. (2001). The prediction of dispersed flows boundaries in liquid-liquid and gas-liquid systems. International Journal of Multiphase Flow 27(5), 885-910. https://doi.org/10.1016/S0301-9322(00)00056-2 (not read; see the confidence note in (e)).
7. Angeli, P., Hewitt, G.F. (2000). Drop size distributions in horizontal oil-water dispersed flows. Chemical Engineering Science 55(16), 3133-3143. https://doi.org/10.1016/S0009-2509(99)00585-0 (the ratio d32/d_max = 0.48 as quoted in later work).

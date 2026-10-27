/** Prints R10-06's molar c_p, density, expansion and compressibility of ice Ih at the triple-point pressure. */
public final class Survey {
    public static void main(String[] args) {
        if (!R1006.verify()) System.exit(1);
        double[] ts = {10, 15, 20, 25, 30, 40, 50, 60, 70, 80, 90, 100, 111.6, 120, 140, 160, 180, 200, 220, 240, 250, 260, 263.15, 270, 273.16};
        for (double t : ts) {
            var d = R1006.at(t, R1006.PT);
            var e = R1006.at(t, 101325);
            System.out.printf("T %7.2f  cp %9.5f J/mol K  rho %9.4f  alpha %10.3e  kappa %10.3e  | 1 atm cp %9.5f rho %9.4f  h-h(Tt) %10.3f J/mol%n", t,
                    d.heatCapacity(t) * R1006.M, d.density(), d.expansion(), d.compressibility(), e.heatCapacity(t) * R1006.M, e.density(),
                    (d.enthalpy(t) - R1006.at(R1006.TT, R1006.PT).enthalpy(R1006.TT)) * R1006.M);
        }
    }
}

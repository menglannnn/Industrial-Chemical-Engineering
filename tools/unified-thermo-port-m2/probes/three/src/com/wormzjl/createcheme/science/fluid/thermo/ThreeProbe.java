package com.wormzjl.createcheme.science.fluid.thermo;

import com.wormzjl.createcheme.science.material.MaterialCatalog;
import com.wormzjl.createcheme.science.thermo.TangentPlaneStability;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * M2 probe of the THREE_PHASES holds of the main-only hold-class worlds: replays each dumped (T, P, overall) through the
 * network's engine with the instrumented stability test, then with the traces below a relative cut removed.
 * Usage: ThreeProbe <states.txt> [maxStates]; a states line is "T P [n0, n1, ...] detail".
 */
public final class ThreeProbe {
    public static void main(String[] args) throws Exception {
        var model = FluidThermodynamics.forNetwork(MaterialCatalog.bundled(), "createcheme:tjl20_methane_nitrogen");
        List<String> names = model.hydrocarbon.components();
        int limit = args.length > 1 ? Integer.parseInt(args[1]) : Integer.MAX_VALUE, seen = 0;
        for (String line : Files.readAllLines(Path.of(args[0]))) {
            if (line.isBlank() || seen++ >= limit) continue;
            String[] head = line.split(" ", 3);
            double t = Double.parseDouble(head[0]), p = Double.parseDouble(head[1]);
            String list = line.substring(line.indexOf('[') + 1, line.indexOf(']'));
            double[] overall = Arrays.stream(list.split(",")).mapToDouble(s -> Double.parseDouble(s.trim())).toArray();
            double total = Arrays.stream(overall).sum();
            System.out.println("=== T=" + t + " P=" + p + " total=" + total);
            StringBuilder c = new StringBuilder();
            for (int i = 0; i < names.size(); i++) if (overall[i] > 0) c.append(' ').append(names.get(i)).append('=').append(String.format("%.3e", overall[i] / total));
            System.out.println(" z:" + c);
            TangentPlaneStability.PROBE = true;
            var r = model.phaseEngine().tp(t, p, overall, false);
            TangentPlaneStability.PROBE = false;
            System.out.println(" RESULT " + r.status() + " " + r.classification() + " " + r.detail());
            var d = r.diagnostics();
            System.out.println(" feed " + d.feedVerdict() + " tm=" + d.feedTangentPlaneDistance() + " tie=" + d.tieLine() + " residual=" + d.fugacityResidual());
            for (double cut : new double[] {1e-6, 1e-8, 1e-10, 1e-12, 1e-14}) {
                double[] trimmed = overall.clone();
                for (int i = 0; i < trimmed.length; i++) if (trimmed[i] < cut * total) trimmed[i] = 0;
                var v = model.phaseEngine().tp(t, p, trimmed, false);
                System.out.println(" cut " + cut + ": " + v.status() + " " + v.classification() + " " + (v.detail().length() > 120 ? v.detail().substring(0, 120) : v.detail()));
            }
        }
    }
}

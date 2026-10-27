import com.wormzjl.createcheme.science.column.v3.V3ColumnCalculator;
import com.wormzjl.createcheme.science.column.v3.V3ColumnInput;
import com.wormzjl.createcheme.science.column.v3.V3ColumnOutcome;
import com.wormzjl.createcheme.world.level.block.entity.ColumnCalculatorV3BlockEntity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.function.Supplier;

/**
 * Column solves for the pow performance study (batch 2026-09-27-jdk-determinism): the literature CDU preset in the
 * prescribed-drop mode (V3LiteraturePresetTest), the shipped literature preset with tray hydraulics, and the methane
 * preset. {@code java ColumnProbe digest} prints per case the outcome, the solver counters and a SHA-256 of the result
 * record (every double printed exactly), so two builds can be compared bit for bit; {@code java ColumnProbe time <reps>}
 * prints the median wall ms per case after one warm-up solve (single thread).
 */
public final class ColumnProbe {
    record Case(String name, Supplier<V3ColumnInput> input) {}
    static final Case[] CASES = {
            new Case("literature-prescribed-drop", () -> ColumnCalculatorV3BlockEntity.literatureCduInput().withColumnDiameter(0.0)),
            new Case("literature-tray-hydraulics", ColumnCalculatorV3BlockEntity::literatureCduInput),
            new Case("methane", ColumnCalculatorV3BlockEntity::methaneCduInput),
    };

    static V3ColumnOutcome solve(Case c) { return V3ColumnCalculator.calculate(c.input().get(), () -> {}, 0.0); }

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "digest";
        if (mode.equals("digest")) {
            for (Case c : CASES) {
                V3ColumnOutcome o = solve(c);
                var d = o.diagnostics();
                String body = o instanceof V3ColumnOutcome.Success s ? s.result().toString()
                        : ((V3ColumnOutcome.Failure) o).code() + " " + ((V3ColumnOutcome.Failure) o).summary();
                String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body.getBytes(StandardCharsets.UTF_8))).substring(0, 16);
                System.out.printf("COLUMN %s %s newton=%d residuals=%d linear=%d init=%d maxScaled=%s result=%s%n", c.name(),
                        o.getClass().getSimpleName(), d.newtonIterations(), d.residualEvaluations(), d.linearSolves(),
                        d.initializerIterations(), Double.toHexString(d.maximumScaledResidual()), digest);
            }
        } else {
            int reps = args.length > 1 ? Integer.parseInt(args[1]) : 5;
            for (Case c : CASES) {
                solve(c);
                long[] ms = new long[reps];
                for (int r = 0; r < reps; r++) { long t0 = System.nanoTime(); solve(c); ms[r] = (System.nanoTime() - t0) / 1_000_000; }
                Arrays.sort(ms);
                System.out.printf("COLUMN_TIME %s median=%d ms runs=%s%n", c.name(), ms[reps / 2], Arrays.toString(ms));
            }
        }
    }
}

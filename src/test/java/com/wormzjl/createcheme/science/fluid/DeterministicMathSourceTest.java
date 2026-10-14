package com.wormzjl.createcheme.science.fluid;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Cross-JDK determinism guard (batch 2026-09-27-jdk-determinism). The fluid solver's results must be bitwise identical
 * on every Java runtime. {@code + - * /}, {@code Math.sqrt} and {@code Math.fma} are correctly rounded by the JLS on
 * every JVM, but {@code Math.log/exp/pow/cbrt/...} only promise 1-2 ulp and differ between HotSpot's intrinsics,
 * OpenJ9, Graal and JDK versions; {@code StrictMath} (fdlibm) is specified bit for bit. The science code calls these
 * functions through {@link com.wormzjl.createcheme.science.math.DeterministicMath} (StrictMath results, with an
 * allocation-free {@code pow}). This test fails when a non-correctly-rounded {@code java.lang.Math} or
 * {@code java.lang.StrictMath} function (or {@code Math.random}) is called directly in the scanned sources.
 *
 * <p>Scanned: {@code science/} and {@code runtime/} of the main sources, and the exact-regression fixtures of
 * {@code fluid/benchmark} in the test sources. Exempt: the facade itself.
 */
class DeterministicMathSourceTest {
    /** java.lang.Math functions that are not correctly rounded (or not deterministic at all). */
    static final String BANNED = "sin|cos|tan|asin|acos|atan2|atan|sinh|cosh|tanh|exp|expm1|log|log10|log1p|pow|cbrt|hypot|random";

    private static final Pattern CALL = Pattern.compile(
            "(?<![A-Za-z0-9_$])(?:StrictMath|Math)\\s*(?:\\.\\s*(" + BANNED + ")\\s*\\(|::\\s*(" + BANNED + ")\\b)");
    private static final Pattern STATIC_IMPORT = Pattern.compile(
            "import\\s+static\\s+java\\.lang\\.(?:Strict)?Math\\.(\\*|(?:" + BANNED + ")\\s*;)");

    private static final Path MAIN = Path.of("src/main/java/com/wormzjl/createcheme");
    private static final Path TEST = Path.of("src/test/java/com/wormzjl/createcheme");
    private static final List<Path> ROOTS = List.of(MAIN.resolve("science"), MAIN.resolve("runtime"),
            TEST.resolve("fluid/benchmark"));
    /** The facade itself, the only file that may call StrictMath's functions. */
    private static final Path FACADE = MAIN.resolve("science/math/DeterministicMath.java");

    @Test void fluidSourcesCallNoNonCorrectlyRoundedMathFunction() throws IOException {
        List<String> violations = new ArrayList<>();
        int scanned = 0;
        for (Path root : ROOTS) {
            assertTrue(Files.isDirectory(root), "scanned root is missing (moved?): " + root);
            List<Path> files;
            try (Stream<Path> walk = Files.walk(root)) {
                files = walk.filter(p -> p.toString().endsWith(".java")).sorted().toList();
            }
            for (Path file : files) {
                if (file.equals(FACADE)) {
                    continue;
                }
                scanned++;
                violations.addAll(violations(file, Files.readString(file)));
            }
        }
        // The scanned roots hold 205 files at 2026-09-27; a much smaller count means the scan lost its roots.
        assertTrue(scanned >= 200, "only " + scanned + " files scanned");
        assertTrue(Files.isRegularFile(FACADE), "facade is missing (moved?): " + FACADE);
        assertTrue(violations.isEmpty(), "use DeterministicMath (bitwise identical on every JVM) instead of:\n"
                + String.join("\n", violations));
    }

    @Test void scannerFindsBannedCallsAndIgnoresAllowedOnes() {
        assertCount(1, violations(Path.of("A.java"), "double x = Math.log(y);"));
        assertCount(1, violations(Path.of("A.java"), "double x = java.lang.Math.pow(y, 2);"));
        assertCount(1, violations(Path.of("A.java"), "f(Math :: exp);"));
        assertCount(1, violations(Path.of("A.java"), "double x = Math . cbrt (y);"));
        assertCount(1, violations(Path.of("A.java"), "import static java.lang.Math.*;"));
        assertCount(1, violations(Path.of("A.java"), "import static java.lang.Math.expm1;"));
        assertCount(1, violations(Path.of("A.java"), "double x = StrictMath.log(y);"));
        assertCount(1, violations(Path.of("A.java"), "import static java.lang.StrictMath.pow;"));
        assertCount(0, violations(Path.of("A.java"), "double x = DeterministicMath.log(y) + Math.sqrt(z) + Math.fma(a, b, c);"));
        assertCount(0, violations(Path.of("A.java"), "double x = StrictMath.sqrt(z) + Math.scalb(y, 3) + Math.ulp(y);"));
        assertCount(0, violations(Path.of("A.java"), "double x = Math.abs(Math.max(a, b)) + Math.logarithmic;"));
        assertCount(0, violations(Path.of("A.java"), "// Math.log(x) in a comment\n/* Math.exp(y) */ int a;"));
        assertCount(1, violations(Path.of("A.java"), "char q = '\"'; double x = Math.exp(y); String s = \"//\";"));
    }

    private static List<String> violations(Path file, String source) {
        String code = stripComments(source);
        List<String> found = new ArrayList<>();
        for (Pattern pattern : List.of(CALL, STATIC_IMPORT)) {
            Matcher m = pattern.matcher(code);
            while (m.find()) {
                int line = 1;
                for (int i = 0; i < m.start(); i++) {
                    if (code.charAt(i) == '\n') {
                        line++;
                    }
                }
                found.add(file + ":" + line + ": " + m.group().trim());
            }
        }
        return found;
    }

    private static void assertCount(int expected, List<String> found) {
        assertEquals(expected, found.size(), found::toString);
    }

    /** Blanks block and line comments, keeping newlines so line numbers stay right (string literals are left as is). */
    private static String stripComments(String source) {
        StringBuilder out = new StringBuilder(source.length());
        int i = 0;
        boolean inString = false;
        while (i < source.length()) {
            char c = source.charAt(i);
            if (inString) {
                out.append(c);
                if (c == '\\' && i + 1 < source.length()) {
                    out.append(source.charAt(++i));
                } else if (c == '"') {
                    inString = false;
                }
                i++;
            } else if (c == '\'') {
                int end = i + 1;
                while (end < source.length() && source.charAt(end) != '\'') {
                    end += source.charAt(end) == '\\' ? 2 : 1;
                }
                out.append(source, i, Math.min(end + 1, source.length()));
                i = end + 1;
            } else if (c == '"') {
                inString = true;
                out.append(c);
                i++;
            } else if (source.startsWith("//", i)) {
                while (i < source.length() && source.charAt(i) != '\n') {
                    i++;
                }
            } else if (source.startsWith("/*", i)) {
                int end = source.indexOf("*/", i + 2);
                end = end < 0 ? source.length() : end + 2;
                for (int k = i; k < end; k++) {
                    if (source.charAt(k) == '\n') {
                        out.append('\n');
                    }
                }
                i = end;
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }
}

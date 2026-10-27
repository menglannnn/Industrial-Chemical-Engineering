import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;

/**
 * Stand-in for the JUnit console launcher (not in the Gradle cache on the Windows machine; no download needed):
 * {@code java -cp <test;main;jars> MiniLauncher package:x.y class:x.y.ZTest method:x.y.ZTest#name exclude:.*Regex}.
 * Prints each failure and a summary line; exits 1 on any failure. Batch 2026-09-27-jdk-determinism (pow performance).
 */
public final class MiniLauncher {
    public static void main(String[] args) {
        List<DiscoverySelector> selectors = new ArrayList<>();
        List<String> excludes = new ArrayList<>();
        for (String a : args) {
            if (a.startsWith("package:")) selectors.add(selectPackage(a.substring(8)));
            else if (a.startsWith("class:")) selectors.add(selectClass(a.substring(6)));
            else if (a.startsWith("method:")) selectors.add(selectMethod(a.substring(7)));
            else if (a.startsWith("exclude:")) excludes.add(a.substring(8));
            else throw new IllegalArgumentException(a);
        }
        var builder = LauncherDiscoveryRequestBuilder.request().selectors(selectors);
        if (!excludes.isEmpty()) builder.filters(org.junit.platform.engine.discovery.ClassNameFilter.excludeClassNamePatterns(excludes.toArray(String[]::new)));
        LauncherDiscoveryRequest request = builder.build();
        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        long t0 = System.nanoTime();
        launcher.execute(request, listener);
        long wall = (System.nanoTime() - t0) / 1_000_000;
        TestExecutionSummary s = listener.getSummary();
        PrintWriter out = new PrintWriter(System.out, true);
        s.printFailuresTo(out, 8);
        out.printf("MINILAUNCHER tests found=%d succeeded=%d failed=%d skipped=%d aborted=%d wallMs=%d%n",
                s.getTestsFoundCount(), s.getTestsSucceededCount(), s.getTestsFailedCount(), s.getTestsSkippedCount(),
                s.getTestsAbortedCount(), wall);
        System.exit(s.getTestsFailedCount() + s.getTestsAbortedCount() > 0 || s.getTestsFoundCount() == 0 ? 1 : 0);
    }
}

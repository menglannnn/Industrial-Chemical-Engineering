import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

/**
 * Runs JUnit 5 test classes one after the other without Gradle (M1 of the unified thermo port): a header per class,
 * the class's own printed output, one line per test method, and a summary. Used to compare the printed numbers of the
 * engine's test families on branch 9aaafd4 and on the port, class by class.
 * Usage: java -cp ... Launch <fully.qualified.TestClass>...
 */
public final class Launch {
    public static void main(String[] args) {
        Launcher launcher = LauncherFactory.create();
        PrintStream out = System.out;
        int totalTests = 0, totalFailed = 0;
        List<String> failedClasses = new ArrayList<>();
        for (String name : args) {
            out.println("### " + name);
            int[] counts = new int[2];
            LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                    .selectors(DiscoverySelectors.selectClass(name)).build();
            launcher.execute(request, new TestExecutionListener() {
                @Override public void executionFinished(TestIdentifier id, TestExecutionResult result) {
                    if (!id.isTest() && result.getStatus() == TestExecutionResult.Status.SUCCESSFUL) return;
                    if (id.isTest()) counts[0]++;
                    if (result.getStatus() != TestExecutionResult.Status.SUCCESSFUL) {
                        if (id.isTest()) counts[1]++;
                        out.println("FAILED " + id.getDisplayName() + ": " + result.getThrowable().map(Throwable::toString).orElse("?"));
                        result.getThrowable().ifPresent(t -> {
                            StackTraceElement[] trace = t.getStackTrace();
                            for (int i = 0; i < Math.min(8, trace.length); i++) out.println("      at " + trace[i]);
                        });
                    }
                }
            });
            out.println("### " + name + " tests=" + counts[0] + " failed=" + counts[1]);
            totalTests += counts[0];
            totalFailed += counts[1];
            if (counts[1] > 0) failedClasses.add(name);
        }
        out.println("### total tests=" + totalTests + " failed=" + totalFailed + (failedClasses.isEmpty() ? "" : " in " + failedClasses));
    }
}

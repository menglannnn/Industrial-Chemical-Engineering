import org.junit.platform.launcher.*;
import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.*;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestExecutionResult;
import java.util.*;

/** Runs selected JUnit 5 tests outside Gradle (M3 javac lane): args are "pkg.Class" or "pkg.Class#method". */
public class M3Run {
    public static void main(String[] args) {
        List<DiscoverySelector> selectors=new ArrayList<>();
        for(String a:args)selectors.add(a.contains("#")?DiscoverySelectors.selectMethod(a):DiscoverySelectors.selectClass(a));
        var request=LauncherDiscoveryRequestBuilder.request().selectors(selectors).build();
        var launcher=LauncherFactory.create();var summary=new SummaryGeneratingListener();
        TestExecutionListener printer=new TestExecutionListener(){
            @Override public void executionFinished(TestIdentifier id,TestExecutionResult result){
                if(!id.isTest())return;
                String u=id.getUniqueId();int at=u.indexOf("[class:");String cls=at<0?"":u.substring(at+7,u.indexOf(']',at));cls=cls.substring(cls.lastIndexOf('.')+1);
                System.out.println("M3RUN "+result.getStatus()+" "+cls+" "+id.getDisplayName());
                result.getThrowable().ifPresent(t->{System.out.println("M3RUN   "+t);for(var e:t.getStackTrace()){if(e.getClassName().startsWith("com.wormzjl"))System.out.println("M3RUN     at "+e);}
                    for(Throwable c=t.getCause();c!=null;c=c.getCause()){System.out.println("M3RUN   caused by "+c);for(var e:c.getStackTrace()){if(e.getClassName().startsWith("com.wormzjl"))System.out.println("M3RUN     at "+e);}}});
            }
        };
        launcher.execute(request,summary,printer);
        var s=summary.getSummary();
        System.out.println("M3RUN SUMMARY found="+s.getTestsFoundCount()+" succeeded="+s.getTestsSucceededCount()+" failed="+s.getTestsFailedCount()+" skipped="+s.getTestsSkippedCount()+" aborted="+s.getTestsAbortedCount());
        System.exit(s.getTotalFailureCount()==0?0:1);
    }
}

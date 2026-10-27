import java.lang.instrument.*;
import java.nio.file.*;
import com.sun.tools.attach.VirtualMachine;
public class ReloadAgent {
 public static void agentmain(String file,Instrumentation instrumentation) throws Exception {
  for(Class<?> type:instrumentation.getAllLoadedClasses())if(type.getName().equals("com.wormzjl.icheme.world.level.block.entity.FluidDeviceBlockEntity")){
   instrumentation.redefineClasses(new ClassDefinition(type,Files.readAllBytes(Path.of(file))));
   System.out.println("PROCESS_CONTROL_HOTSWAP replaced "+type.getName());return;
  }
  throw new IllegalStateException("Target class not loaded");
 }
 public static void main(String[] args) throws Exception {
  VirtualMachine vm=VirtualMachine.attach(args[0]);try{vm.loadAgent(args[1],args[2]);}finally{vm.detach();}
 }
}

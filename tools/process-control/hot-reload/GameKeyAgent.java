import java.lang.instrument.*;
public class GameKeyAgent {
 public static void agentmain(String value,Instrumentation instrumentation) throws Exception {
  ClassLoader loader=null;for(Class<?> t:instrumentation.getAllLoadedClasses())if(t.getName().equals("com.wormzjl.icheme.world.level.block.entity.FluidDeviceBlockEntity")){loader=t.getClassLoader();break;}
  Class<?> game=Class.forName("net.minecraft.client.Minecraft",true,loader);Object client=game.getMethod("getInstance").invoke(null);
  Runnable action=()->{try{Object options=game.getField("options").get(client);Object key=options.getClass().getField("keyShift").get(options);key.getClass().getMethod("setDown",boolean.class).invoke(key,Boolean.parseBoolean(value));System.out.println("PROCESS_CONTROL_GAME_SHIFT "+value);}catch(Exception e){throw new RuntimeException(e);}};
  game.getMethod("executeBlocking",Runnable.class).invoke(client,action);
 }
}

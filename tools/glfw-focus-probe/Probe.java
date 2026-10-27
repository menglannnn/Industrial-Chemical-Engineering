import static org.lwjgl.glfw.GLFW.*;
import java.awt.MouseInfo;
import java.awt.Point;

/** Does glfwSetCursorPos / CURSOR_DISABLED move the OS cursor while the window is NOT foreground? */
public class Probe {
    static Point p() { return MouseInfo.getPointerInfo().getLocation(); }
    static void pump(long w, int ms) throws Exception { long end = System.currentTimeMillis() + ms; while (System.currentTimeMillis() < end) { glfwPollEvents(); Thread.sleep(10); } }
    static long fg() { try { var c = Class.forName("org.lwjgl.system.windows.User32"); return (long) c.getMethod("GetForegroundWindow").invoke(null); } catch (Throwable t) { return -1; } }
    public static void main(String[] a) throws Exception {
        if (!glfwInit()) throw new IllegalStateException("glfwInit");
        Point orig = p();
        long w = glfwCreateWindow(400, 300, "glfw focus probe", 0, 0);
        glfwSetWindowPos(w, 60, 60);
        pump(w, 1200);
        long hwnd = org.lwjgl.glfw.GLFWNativeWin32.glfwGetWin32Window(w);
        System.out.println("GLFW_FOCUSED=" + glfwGetWindowAttrib(w, GLFW_FOCUSED) + " foreground=" + (fg() == -1 ? "n/a" : (fg() == hwnd ? "THIS WINDOW" : "other window")));
        System.out.println("cursor before=" + orig);
        glfwSetCursorPos(w, 200, 150);           // what the bridge's scroll_at / mouse_drag / vanilla releaseMouse do
        Thread.sleep(150); Point afterSet = p();
        System.out.println("after glfwSetCursorPos(200,150) -> " + afterSet + (afterSet.equals(orig) ? "  (OS cursor NOT moved)" : "  (OS cursor MOVED)"));
        glfwSetCursorPos(w, orig.x - 60, orig.y - 60 - 0); Thread.sleep(100);
        Point beforeDis = p();
        glfwSetInputMode(w, GLFW_CURSOR, GLFW_CURSOR_DISABLED);   // what exit_control_mode / vanilla grabMouse do
        pump(w, 400); Point afterDis = p();
        System.out.println("after CURSOR_DISABLED -> " + afterDis + (afterDis.equals(beforeDis) ? "  (NOT moved)" : "  (MOVED/locked)"));
        glfwSetInputMode(w, GLFW_CURSOR, GLFW_CURSOR_NORMAL); pump(w, 100);
        glfwSetCursorPos(w, orig.x - 60, orig.y - 60); Thread.sleep(50);
        System.out.println("restored=" + p());
        glfwDestroyWindow(w); glfwTerminate();
    }
}

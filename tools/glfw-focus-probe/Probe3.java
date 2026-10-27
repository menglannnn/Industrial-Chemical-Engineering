import static org.lwjgl.glfw.GLFW.*;
/** No cursor calls: report GLFW's focus flag vs. the real foreground window, every 500 ms for 4 s. */
public class Probe3 {
    public static void main(String[] a) throws Exception {
        glfwInit();
        long w = glfwCreateWindow(400, 300, "glfw focus probe 3", 0, 0);
        glfwSetWindowFocusCallback(w, (win, f) -> System.out.println("  focus callback: " + f));
        System.out.println("HWND=" + org.lwjgl.glfw.GLFWNativeWin32.glfwGetWin32Window(w));
        for (int i = 0; i < 8; i++) { long end = System.currentTimeMillis() + 500; while (System.currentTimeMillis() < end) { glfwPollEvents(); Thread.sleep(10); } System.out.println("t=" + (i + 1) * 500 + "ms GLFW_FOCUSED=" + glfwGetWindowAttrib(w, GLFW_FOCUSED)); }
        glfwDestroyWindow(w); glfwTerminate();
    }
}

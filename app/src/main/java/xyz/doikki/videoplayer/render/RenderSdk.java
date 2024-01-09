package xyz.doikki.videoplayer.render;

public class RenderSdk {
    private static boolean sLoadLibrary = false;

    static {
        load();
    }

    public static void load() {
        if (sLoadLibrary) {
            return;
        }
        System.loadLibrary("glrender");
        System.loadLibrary("c++_shared");
        sLoadLibrary = true;
    }

    public static IRenderSdkProcess createRenderProcess() {
        load();
        return new RenderSdkProcessImpl();
    }
}

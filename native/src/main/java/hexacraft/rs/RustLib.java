package hexacraft.rs;

import java.util.concurrent.atomic.AtomicBoolean;

public class RustLib {
    private static final AtomicBoolean hasLoadedNative = new AtomicBoolean(false);
    static void loadNative() {
        if (!hasLoadedNative.getAndSet(true)) {
            NativeLoader.load("hexacraft_rs");
        }
    }
    static {
        RustLib.loadNative();
    }

    public static native String hello();

    public static class PerlinNoise4D {
        static {
            RustLib.loadNative();
        }

        public static native long init(int[] perm);
        public static native double noise(long handle, double xx, double yy, double zz, double ww);
    }
}

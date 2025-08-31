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

    public static class NoiseGenerator4D {
        static {
            RustLib.loadNative();
        }

        public static native long storePerms(int[] perm);
        public static native long createLayeredNoiseGenerator(long[] noiseHandles);
        public static native double genNoise(long genHandle, double scale, double x, double y, double z, double w);
    }
}

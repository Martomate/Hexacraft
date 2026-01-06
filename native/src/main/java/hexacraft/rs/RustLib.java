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

    public static class NoiseGenerator3D {
        static {
            RustLib.loadNative();
        }

        public static native long storePerms(int[] perm);
        public static native long createLayeredNoiseGenerator(long[] noiseHandles);
        public static native double genNoise(long genHandle, double scale, double x, double y, double z);
    }

    public static class NoiseGenerator4D {
        static {
            RustLib.loadNative();
        }

        public static native long storePerms(int[] perm);
        public static native long createLayeredNoiseGenerator(long[] noiseHandles);
        public static native double genNoise(long genHandle, double scale, double x, double y, double z, double w);
    }

    public static class VorbisDecoder {
        static {
            RustLib.loadNative();
        }

        public static native long decode(byte[] bytes);
        public static native short[] getSamples(long handle);
        public static native int getSampleRate(long handle);
        public static native void destroy(long handle);
    }

    public static class ClientSocket {
        static {
            RustLib.loadNative();
        }

        public static native long create(byte[] clientId);
        public static native void connect(long handle, String host, int port) throws RuntimeException;
        public static native void send(long handle, byte[] data) throws RuntimeException;
        public static native byte[] receive(long handle) throws RuntimeException;
        public static native void close(long handle);
    }

    public static class ServerSocket {
        static {
            RustLib.loadNative();
        }

        public static native long create();
        public static native void bind(long handle, int port) throws RuntimeException;
        public static native void send(long handle, byte[] clientId, byte[] data) throws RuntimeException;
        public static native byte[] receive(long handle) throws RuntimeException;
        public static native void close(long handle);
    }
}

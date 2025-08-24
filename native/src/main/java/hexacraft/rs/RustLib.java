package hexacraft.rs;

public class RustLib {
    static {
        NativeLoader.load("hexacraft_rs");
    }

    native static String hello();
}

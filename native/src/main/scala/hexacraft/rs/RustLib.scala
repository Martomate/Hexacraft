package hexacraft.rs

object RustLib {
  NativeLoader.load("hexacraft_rs")

  @native def hello(): String
}

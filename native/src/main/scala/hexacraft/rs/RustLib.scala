package hexacraft.rs

import com.github.sbt.jni.syntax.NativeLoader

object RustLib extends NativeLoader("hexacraft_rs") {
  @native def hello(): String
}

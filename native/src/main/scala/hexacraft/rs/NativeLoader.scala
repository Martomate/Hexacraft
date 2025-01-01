package hexacraft.rs

import java.nio.file.{Files, Path}

object NativeLoader {
  private val (os, arch) = determinePlatform()

  def load(libraryName: String): Unit = {
    val tmp: Path = Files.createTempDirectory("jni-")
    tmp.toFile.deleteOnExit()

    val fileName: String = System.mapLibraryName(libraryName)
    val resourcePath: String = s"/native/$arch-$os/$fileName"
    val extractedPath = tmp.resolve(fileName)

    extractLibrary(fileName, resourcePath, extractedPath)

    System.load(extractedPath.toAbsolutePath.toString)
  }

  private def extractLibrary(name: String, resourcePath: String, destination: Path) = {
    val resourceStream = this.getClass.getResourceAsStream(resourcePath)
    if resourceStream == null then {
      throw new UnsatisfiedLinkError(s"Native library $name ($resourcePath) cannot be found on the classpath.")
    }

    try {
      Files.copy(resourceStream, destination)
    } catch {
      case ex: Exception => throw new UnsatisfiedLinkError(s"Error while extracting native library: $ex")
    }
  }

  private def determinePlatform(): (String, String) = {
    val os = System.getProperty("os.name").toLowerCase match {
      case s if s.contains("win") => "windows"
      case s if s.contains("mac") => "darwin"
      case _                      => "linux"
    }

    val arch = System.getProperty("os.arch").toLowerCase match {
      case "arm64" | "aarch64" => "arm64"
      case arch                => "x86_64"
    }

    (os, arch)
  }
}

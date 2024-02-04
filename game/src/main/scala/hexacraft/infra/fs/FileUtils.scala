package hexacraft.infra.fs

import java.io.{BufferedReader, InputStreamReader}
import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.util.jar.JarFile
import scala.collection.mutable

object FileUtils {
  def getResourceFile(path: String): Option[URL] = {
    Option(getClass.getResource("/" + path))
  }

  def listFilesInResource(path: URL): java.util.stream.Stream[String] = {
    if path.toURI.getScheme.equalsIgnoreCase("file") then {
      Files
        .list(Paths.get(path.toURI))
        .filter(p => Files.isRegularFile(p))
        .map(_.getFileName.toString)
    } else {
      val pathStr =
        java.net.URLDecoder.decode(path.getPath.substring(5), StandardCharsets.UTF_8.name())
      val jarSepIndex = pathStr.indexOf('!')
      val jar = new JarFile(pathStr.substring(0, jarSepIndex))
      val query = pathStr.substring(jarSepIndex + 2)
      jar
        .stream()
        .map[String](_.getName)
        .filter(_.startsWith(query))
        .map[String](_.substring(query.length))
        .filter(n => n.nonEmpty && n.indexOf('/') == -1)
    }
  }

  def getBufferedReader(url: URL): BufferedReader = {
    new BufferedReader(new InputStreamReader(url.openStream()))
  }

  def readLinesFromUrl(url: URL): Seq[String] = {
    val lines = mutable.ArrayBuffer.empty[String]

    val reader = getBufferedReader(url)
    reader.lines().forEach(lines += _)
    reader.close()

    lines.toSeq
  }

  def readBytesFromUrl(url: URL): Array[Byte] = {
    val stream = url.openStream()
    try {
      stream.readAllBytes()
    } finally {
      stream.close()
    }
  }
}

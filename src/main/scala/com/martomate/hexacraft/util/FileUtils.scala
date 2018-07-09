package com.martomate.hexacraft.util

import java.io.{BufferedReader, InputStreamReader}
import java.net.URL
import java.nio.file.{Files, Paths}
import java.util.jar.JarFile

object FileUtils {

  def getResourceFile(path: String): Option[URL] = {
    val url = getClass.getResource("/" + path)
    Option(url)
  }

  def listFilesInResource(path: URL): java.util.stream.Stream[String] = {
    if (path.toURI.getScheme.equalsIgnoreCase("file")) {
      Files.list(Paths.get(path.toURI))
        .filter(p => Files.isRegularFile(p))
        .map(_.getFileName.toString)
    } else {
      val pathStr = path.getPath.substring(5)
      val jarSepIndex = pathStr.indexOf('!')
      val jar = new JarFile(pathStr.substring(0, jarSepIndex))
      val query = pathStr.substring(jarSepIndex + 2)
      jar.stream()
        .map[String](_.getName)
        .filter(_.startsWith(query))
        .map[String](_.substring(query.length))
        .filter(n => n.nonEmpty && n.indexOf('/') == -1)
    }
  }

  def getBufferedReader(url: URL) = new BufferedReader(new InputStreamReader(url.openStream()))

}

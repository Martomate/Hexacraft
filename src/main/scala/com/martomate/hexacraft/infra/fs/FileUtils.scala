package com.martomate.hexacraft.infra.fs

import java.io.{BufferedReader, InputStreamReader}
import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.util.jar.JarFile

object FileUtils:
  def getResourceFile(path: String): Option[URL] =
    Option(getClass.getResource("/" + path))

  def listFilesInResource(path: URL): java.util.stream.Stream[String] =
    if path.toURI.getScheme.equalsIgnoreCase("file")
    then
      Files
        .list(Paths.get(path.toURI))
        .filter(p => Files.isRegularFile(p))
        .map(_.getFileName.toString)
    else
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

  def getBufferedReader(url: URL) = new BufferedReader(new InputStreamReader(url.openStream()))

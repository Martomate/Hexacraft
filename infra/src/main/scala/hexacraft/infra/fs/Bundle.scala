package hexacraft.infra.fs

import java.awt.image.BufferedImage
import java.io.{BufferedReader, InputStreamReader}
import java.net.URL
import javax.imageio.ImageIO
import scala.collection.mutable

/** Files in the `resources` folder can be read using this class */
object Bundle {
  def locate(path: String): Option[Resource] = {
    Option(getClass.getResource("/" + path)).map(url => Resource(url))
  }

  class Resource(url: URL) {
    def readLines(): Seq[String] = {
      val lines = mutable.ArrayBuffer.empty[String]

      val reader = new BufferedReader(new InputStreamReader(url.openStream()))
      reader.lines().forEach(lines += _)
      reader.close()

      lines.toSeq
    }

    def readBytes(): Array[Byte] = {
      val stream = url.openStream()
      try {
        stream.readAllBytes()
      } finally {
        stream.close()
      }
    }

    def readImage(): BufferedImage = {
      ImageIO.read(url)
    }
  }
}

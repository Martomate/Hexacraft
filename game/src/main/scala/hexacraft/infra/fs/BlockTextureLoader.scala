package hexacraft.infra.fs

import hexacraft.renderer.PixelArray

import java.awt.image.BufferedImage
import scala.collection.mutable

trait BlockTextureLoader {
  def load(squareTextureNames: Seq[String], triTextureNames: Seq[String]): BlockTextureLoader.LoadedImages
}

object BlockTextureLoader {
  lazy val instance: BlockTextureLoader = new BlockTextureLoaderImpl()

  private class BlockTextureLoaderImpl extends BlockTextureLoader {
    def load(squareTextureNames: Seq[String], triTextureNames: Seq[String]): LoadedImages = {
      val nameToIdx = mutable.Map.empty[String, Int]
      val images = mutable.ArrayBuffer.empty[PixelArray]

      for (fileName, isTriImage) <- squareTextureNames.map((_, false)) ++ triTextureNames.map((_, true)) do {
        val lastDot = fileName.lastIndexOf('.')
        val name = fileName.substring(0, lastDot)
        nameToIdx += name -> images.size
        images ++= loadImages(Bundle.locate(s"textures/blocks/$fileName").get.readImage(), isTriImage)
      }

      LoadedImages(images.toSeq, nameToIdx.toMap)
    }

    private def loadImages(image: BufferedImage, isTriImage: Boolean): Seq[PixelArray] = {
      val w = image.getWidth
      val h = image.getHeight
      val numImages = w / h
      for i <- 0 until numImages yield {
        PixelArray(image.getRGB(i * h, 0, h, h, null, 0, h), isTriImage)
      }
    }
  }

  class LoadedImages(val images: Seq[PixelArray], val texIdxMap: Map[String, Int])
}

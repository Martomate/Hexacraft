package hexacraft.client

import hexacraft.infra.fs.Bundle
import hexacraft.renderer.PixelArray
import hexacraft.util.Result
import hexacraft.world.block.BlockSpec

import java.awt.image.BufferedImage
import java.io.FileNotFoundException
import scala.collection.mutable

trait BlockTextureLoader {
  def load(fileName: String): Result[Seq[Array[Int]], Throwable]
}

object BlockTextureLoader {
  lazy val instance: BlockTextureLoader = BlockTextureLoaderImpl

  def loadBlockTextures(
      blockSpecs: Map[String, BlockSpec],
      blockLoader: BlockTextureLoader
  ): Result[BlockTextureLoader.LoadedImages, String] = {
    val textures = blockSpecs.values.map(_.textures)
    val squareTexturesNames = textures.flatMap(_.sides).toSet.toSeq
    val triTexturesNames = (textures.map(_.top) ++ textures.map(_.bottom)).toSet.toSeq

    val texturesToLoad = Seq(
      squareTexturesNames.map(TextureToLoad(_, isTriImage = false)),
      triTexturesNames.map(TextureToLoad(_, isTriImage = true))
    ).flatten

    Result
      .all(texturesToLoad)(t =>
        blockLoader
          .load(s"${t.name}.png")
          .map(t.name -> _.map(PixelArray(_, t.isTriImage)))
      )
      .mapErr(e => s"Block texture not found: ${e.getMessage}")
      .map(collectImages)
  }

  class LoadedImages(val images: Seq[PixelArray], val texIdxMap: Map[String, Int])

  private case class TextureToLoad(name: String, isTriImage: Boolean)

  private def collectImages(loadedImages: Seq[(String, Seq[PixelArray])]): LoadedImages = {
    val nameToIdx = mutable.Map.empty[String, Int]
    val images = mutable.ArrayBuffer.empty[PixelArray]

    for (name, im) <- loadedImages do {
      nameToIdx += name -> images.size
      images ++= im
    }

    LoadedImages(images.toSeq, nameToIdx.toMap)
  }

  private object BlockTextureLoaderImpl extends BlockTextureLoader {
    override def load(fileName: String): Result[Seq[Array[Int]], Throwable] = {
      Result
        .fromOption(Bundle.locate(s"textures/blocks/$fileName"))
        .mapErr(_ => FileNotFoundException(s"res://textures/blocks/$fileName"))
        .andThen(res => Result.attempt(loadImages(res.readImage())))
    }

    private def loadImages(image: BufferedImage): Seq[Array[Int]] = {
      val w = image.getWidth
      val h = image.getHeight
      val numImages = w / h
      for i <- 0 until numImages yield {
        image.getRGB(i * h, 0, h, h, null, 0, h)
      }
    }
  }
}

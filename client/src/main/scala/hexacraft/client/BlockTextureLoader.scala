package hexacraft.client

import hexacraft.infra.fs.Bundle
import hexacraft.renderer.PixelArray
import hexacraft.util.Result

import java.awt.image.BufferedImage
import java.io.FileNotFoundException
import scala.collection.mutable

trait BlockTextureLoader {
  def load(
      squareTextureNames: Seq[String],
      triTextureNames: Seq[String]
  ): Result[BlockTextureLoader.LoadedImages, FileNotFoundException]
}

object BlockTextureLoader {
  lazy val instance: BlockTextureLoader = new BlockTextureLoaderImpl()

  class LoadedImages(val images: Seq[PixelArray], val texIdxMap: Map[String, Int])

  object LoadedImages {
    def fromLoadableImages(
        loadableImages: Seq[LoadableImage],
        load: LoadableImage => Seq[PixelArray]
    ): LoadedImages = {
      val nameToIdx = mutable.Map.empty[String, Int]
      val images = mutable.ArrayBuffer.empty[PixelArray]

      for im <- loadableImages do {
        nameToIdx += im.name -> images.size
        images ++= load(im)
      }

      LoadedImages(images.toSeq, nameToIdx.toMap)
    }
  }

  case class LoadableImage(
      name: String,
      isTriImage: Boolean,
      resource: Bundle.Resource
  )

  private class BlockTextureLoaderImpl extends BlockTextureLoader {
    def load(
        squareTextureNames: Seq[String],
        triTextureNames: Seq[String]
    ): Result[LoadedImages, FileNotFoundException] = {
      for {
        squareImages <- Result.all(squareTextureNames)(tryPrepareLoad(_, false))
        triImages <- Result.all(triTextureNames)(tryPrepareLoad(_, true))
      } yield LoadedImages.fromLoadableImages(
        squareImages ++ triImages,
        im => loadImages(im.resource.readImage(), im.isTriImage)
      )
    }

    private def tryPrepareLoad(fileName: String, isTriImage: Boolean): Result[LoadableImage, FileNotFoundException] =
      for {
        resource <- Result
          .fromOption(Bundle.locate(s"textures/blocks/$fileName"))
          .mapErr(_ => FileNotFoundException(s"res://textures/blocks/$fileName"))

        lastDot = fileName.lastIndexOf('.')
        name = fileName.substring(0, lastDot)
      } yield LoadableImage(name, isTriImage, resource)

    private def loadImages(image: BufferedImage, isTriImage: Boolean): Seq[PixelArray] = {
      val w = image.getWidth
      val h = image.getHeight
      val numImages = w / h
      for i <- 0 until numImages yield {
        PixelArray(image.getRGB(i * h, 0, h, h, null, 0, h), isTriImage)
      }
    }
  }
}

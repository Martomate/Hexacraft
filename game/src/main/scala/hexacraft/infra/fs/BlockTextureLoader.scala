package hexacraft.infra.fs

import hexacraft.renderer.PixelArray
import hexacraft.world.block.BlockSpec

import java.net.URL
import javax.imageio.ImageIO
import scala.collection.mutable

trait BlockTextureLoader:
  def reload(): BlockTextureMapping
  def textureMapping: BlockTextureMapping

object BlockTextureLoader:
  lazy val instance: BlockTextureLoader =
    val loader = new BlockTextureLoaderImpl()
    loader.reload()
    loader

  private class BlockTextureLoaderImpl extends BlockTextureLoader:
    private var texIdxMap: Map[String, Int] = _
    private var _latestMapping: BlockTextureMapping = _

    def reload(): BlockTextureMapping =
      val nameToIdx = mutable.Map.empty[String, Int]
      val images = mutable.ArrayBuffer.empty[PixelArray]

      def loadImages(file: URL): Seq[PixelArray] =
        val image = ImageIO.read(file)
        val w = image.getWidth
        val h = image.getHeight
        val numImages = w / h
        for i <- 0 until numImages yield PixelArray(image.getRGB(i * h, 0, h, h, null, 0, h))

      val dir = FileUtils.getResourceFile("textures/blocks/").get
      val files = FileUtils.listFilesInResource(dir).toArray[String](len => new Array(len))
      for fileName <- files do
        val lastDot = fileName.lastIndexOf('.')
        val name = fileName.substring(0, lastDot)
        nameToIdx += name -> images.size
        images ++= loadImages(new URL(dir, fileName))

      texIdxMap = nameToIdx.toMap
      _latestMapping = BlockTextureMapping(images.toSeq, texIdxMap)
      _latestMapping

    override def textureMapping: BlockTextureMapping = _latestMapping

class BlockTextureMapping(val images: Seq[PixelArray], texIdxMap: Map[String, Int]) {
  def textureArrayIndices(spec: BlockSpec): IndexedSeq[Int] =
    val specOpt =
      for texIdxMap <- Option(texIdxMap)
      yield spec.textures.indices(texIdxMap)

    specOpt.getOrElse(IndexedSeq.fill(8)(0))
}

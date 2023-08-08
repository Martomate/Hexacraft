package hexacraft.world.block

import hexacraft.infra.fs.FileUtils
import hexacraft.renderer.PixelArray

import java.net.URL
import javax.imageio.ImageIO
import scala.collection.mutable

trait BlockLoader:
  def reloadAllBlockTextures(): Seq[PixelArray]

  /** @return `(offsets << 12 | texture_array_index)` for each side */
  def loadBlockType(spec: BlockSpec): IndexedSeq[Int]

object BlockLoader:
  lazy val instance: BlockLoader =
    val loader = new BlockLoaderImpl()
    loader.reloadAllBlockTextures()
    loader

  private class BlockLoaderImpl extends BlockLoader:
    private var texIdxMap: Map[String, Int] = _

    def reloadAllBlockTextures(): Seq[PixelArray] =
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
      images.toSeq

    def loadBlockType(spec: BlockSpec): IndexedSeq[Int] = {
      val specOpt =
        for texIdxMap <- Option(texIdxMap)
        yield spec.textures.indices(texIdxMap)

      specOpt.getOrElse(IndexedSeq.fill(8)(0))
    }

package hexacraft.world.block

import com.eclipsesource.json.{Json, JsonObject, JsonValue}
import hexacraft.infra.fs.FileUtils
import hexacraft.renderer.TextureToLoad
import hexacraft.util.ResourceWrapper

import java.net.URL
import javax.imageio.ImageIO
import scala.collection.mutable

trait BlockLoader:
  def reloadAllBlockTextures(): Seq[TextureToLoad]

  /** @return `(offsets << 12 | texture_array_index)` for each side */
  def loadBlockType(spec: BlockSpec): IndexedSeq[Int]

object BlockLoader:
  lazy val instance: BlockLoader =
    val loader = new BlockLoaderImpl()
    loader.reloadAllBlockTextures()
    loader

  private class BlockLoaderImpl extends BlockLoader:
    private var texIdxMap: Map[String, Int] = _

    def reloadAllBlockTextures(): Seq[TextureToLoad] =
      val nameToIdx = mutable.Map.empty[String, Int]
      val images = mutable.ArrayBuffer.empty[TextureToLoad]

      def loadImages(file: URL): Seq[TextureToLoad] =
        val image = ImageIO.read(file)
        val w = image.getWidth
        val h = image.getHeight
        val numImages = w / h
        for i <- 0 until numImages yield TextureToLoad(image.getRGB(i * h, 0, h, h, null, 0, h))

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

package com.martomate.hexacraft.world.block

import com.eclipsesource.json.{Json, JsonObject, JsonValue}
import com.martomate.hexacraft.renderer.{TextureArray, TextureToLoad}
import com.martomate.hexacraft.util.{FileUtils, ResourceWrapper}

import java.net.URL
import javax.imageio.ImageIO
import scala.collection.mutable

trait BlockLoader:

  /** @return `(offsets << 12 | texture_array_index)` for each side */
  def loadBlockType(name: String): IndexedSeq[Int]

object BlockLoader:
  lazy val instance: BlockLoader =
    val _instance = new BlockLoaderImpl()
    TextureArray.registerTextureArray(
      "blocks",
      BlockTexture.blockTextureSize,
      new ResourceWrapper(_instance.loadAllBlockTextures())
    )
    _instance

  private class BlockLoaderImpl extends BlockLoader:
    private var texIdxMap: Map[String, Int] = _

    def loadAllBlockTextures(): Seq[TextureToLoad] =
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

    def loadBlockType(name: String): IndexedSeq[Int] = {
      val specOpt = for
        texIdxMap <- Option(texIdxMap)
        file <- FileUtils.getResourceFile(s"spec/blocks/$name.json")
        reader <- Option(FileUtils.getBufferedReader(file))
      yield
        val base = Json.parse(reader).asObject()
        val spec = BlockSpec.fromJson(base)
        spec.textures.indices(texIdxMap)

      specOpt.getOrElse(IndexedSeq.fill(8)(0))
    }

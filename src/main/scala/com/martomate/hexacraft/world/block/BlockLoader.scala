package com.martomate.hexacraft.world.block

import com.eclipsesource.json.{Json, JsonObject, JsonValue}
import com.martomate.hexacraft.resource.{ResourceWrapper, TextureArray, TextureToLoad}
import com.martomate.hexacraft.util.FileUtils
import com.martomate.hexacraft.world.block.BlockLoader.texIdxMap

import java.net.URL
import javax.imageio.ImageIO

trait IBlockLoader:

  /** @return `(offsets << 12 | texture_array_index)` for each side */
  def loadBlockType(name: String): IndexedSeq[Int]

object BlockLoader extends IBlockLoader:
  private var texIdxMap: Map[String, Int] = _

  def init(): Unit = {
    TextureArray.registerTextureArray(
      "blocks",
      BlockTexture.blockTextureSize,
      new ResourceWrapper(loadAllBlockTextures())
    )
  }

  def loadAllBlockTextures(): Seq[TextureToLoad] = {
    val nameToIdx = collection.mutable.Map.empty[String, Int]
    val images = collection.mutable.ArrayBuffer.empty[TextureToLoad]

    def loadImages(file: URL): Seq[TextureToLoad] = {
      val image = ImageIO.read(file)
      val w = image.getWidth
      val h = image.getHeight
      val numImages = w / h
      for (i <- 0 until numImages) yield TextureToLoad(image.getRGB(i * h, 0, h, h, null, 0, h))
    }

    val dir = FileUtils.getResourceFile("textures/blocks/").get
    val files = FileUtils.listFilesInResource(dir).toArray[String](len => new Array(len))
    for (fileName <- files) {
      val lastDot = fileName.lastIndexOf('.')
      val name = fileName.substring(0, lastDot)
      nameToIdx += name -> images.size
      images ++= loadImages(new URL(dir, fileName))
    }

    texIdxMap = nameToIdx.toMap
    images.toSeq
  }

  def loadBlockType(name: String): IndexedSeq[Int] = {
    val specOpt = for {
      texIdxMap <- Option(texIdxMap)
      file <- FileUtils.getResourceFile(s"spec/blocks/$name.json")
      reader <- Option(FileUtils.getBufferedReader(file))
    } yield {
      val base = Json.parse(reader).asObject()
      val spec = BlockSpec.fromJson(base)
      spec.textures.indices(texIdxMap)
    }
    specOpt.getOrElse(IndexedSeq.fill(8)(0))
  }

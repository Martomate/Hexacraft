package com.martomate.hexacraft.block

import java.net.URL

import com.eclipsesource.json.{Json, JsonValue}
import com.martomate.hexacraft.resource.{ResourceWrapper, TextureArray}
import com.martomate.hexacraft.util.FileUtils
import javax.imageio.ImageIO

object BlockLoader {
  private var texIdxMap: Map[String, Int] = _
  
  def init(): Unit = {
    TextureArray.registerTextureArray("blocks", BlockTexture.blockTextureSize, new ResourceWrapper(loadAllBlockTextures()))
  }
  
  def loadAllBlockTextures(): Seq[Array[Int]] = {
    val nameToIdx = collection.mutable.Map.empty[String, Int]
    val images = collection.mutable.ArrayBuffer.empty[Array[Int]]
    
    def loadImages(file: URL): Seq[Array[Int]] = {
      val image = ImageIO.read(file)
      val w = image.getWidth
      val h = image.getHeight
      val numImages = w / h
      for (i <- 0 until numImages) yield image.getRGB(i * h, 0, h, h, null, 0, h)
    }
    val dir = FileUtils.getResourceFile("textures/blocks/").get
    val files = FileUtils.listFilesInResource(dir).toArray[String](len => new Array(len))
    for (f <- files) {
      val fileName = f
      val lastDot = fileName.lastIndexOf('.')
      val name = fileName.substring(0, lastDot)
      nameToIdx += name -> images.size
      images ++= loadImages(new URL(dir, f))
    }
    
    texIdxMap = nameToIdx.toMap
    images
  }
  
  def loadBlockType(name: String): IndexedSeq[Int] = {
    FileUtils.getResourceFile("spec/blocks/" + name + ".json") match {
      case Some(file) =>
        val reader = FileUtils.getBufferedReader(file)
        if (reader != null) {
          val base = Json.parse(reader).asObject()
          val textures = base.get("textures").asObject()
          val all = textures.get("all")
          val side = textures.get("side") or all
          val topIdx = texIdxMap((textures.get("top") or all).asString())
          val bottomIdx = texIdxMap((textures.get("bottom") or all).asString())
          val sidesIdx = (2 until 8).map(i => texIdxMap((textures.get(s"side$i") or side).asString()))
          topIdx +: bottomIdx +: sidesIdx
        } else IndexedSeq.fill(8)(0)
      case None =>
        IndexedSeq.fill(8)(0)
    }
  }
  
  implicit class DefaultJsonImplicitClass(left: JsonValue) {
    def or(right: JsonValue): JsonValue = if (left == null) right else left
  }
}

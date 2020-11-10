package com.martomate.hexacraft.world.block

import java.net.URL

import com.eclipsesource.json.{Json, JsonObject, JsonValue}
import com.martomate.hexacraft.resource.{ResourceWrapper, TextureArray, TextureToLoad}
import com.martomate.hexacraft.util.FileUtils
import javax.imageio.ImageIO

trait IBlockLoader {
  def loadBlockType(name: String): IndexedSeq[Int]
}

object BlockLoader extends IBlockLoader {
  private var texIdxMap: Map[String, Int] = _
  
  def init(): Unit = {
    TextureArray.registerTextureArray("blocks", BlockTexture.blockTextureSize, new ResourceWrapper(loadAllBlockTextures()))
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
    for (f <- files) {
      val fileName = f
      val lastDot = fileName.lastIndexOf('.')
      val name = fileName.substring(0, lastDot)
      nameToIdx += name -> images.size
      images ++= loadImages(new URL(dir, f))
    }
    
    texIdxMap = nameToIdx.toMap
    images.toSeq
  }

  def offsets(value: JsonValue): Int = {
    if (value == null) 0
    else {
      val obj = value.asObject()

      val off0 = obj.getInt("0", 0)
      val off1 = obj.getInt("1", 0)
      val off2 = obj.getInt("2", 0)
      val off3 = obj.getInt("3", 0)
      val off4 = obj.getInt("4", 0)
      val off5 = obj.getInt("5", 0)

      if (off0 != 0) 0 // not allowed
      else {
        off1 << 16 | off2 << 12 | off3 << 8 | off4 << 4 | off5
      }
    }
  }

  def loadBlockType(name: String): IndexedSeq[Int] = {
    val retOpt = for {
      texIdxMap <- Option(texIdxMap)
      file <- FileUtils.getResourceFile("spec/blocks/" + name + ".json")
      reader <- Option(FileUtils.getBufferedReader(file))
    } yield {
      val base = Json.parse(reader).asObject()
      val textures = base.get("textures").asObject()
      val all = textures.get("all")
      val side = textures.get("side") or all
      val topIdx = texIdxMap((textures.get("top") or all).asString()) | offsets(textures.get("topOffsets")) << 12
      val bottomIdx = texIdxMap((textures.get("bottom") or all).asString()) | offsets(textures.get("bottomOffsets")) << 12
      val sidesIdx = (2 until 8).map(i => texIdxMap((textures.get(s"side$i") or side).asString()))
      topIdx +: bottomIdx +: sidesIdx
    }
    retOpt getOrElse IndexedSeq.fill(8)(0)
  }

  implicit class DefaultJsonImplicitClass(left: JsonValue) {
    def or(right: JsonValue): JsonValue = if (left == null) right else left
  }
}

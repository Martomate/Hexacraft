package hexagon.block

import java.io.File
import java.io.FileReader
import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonValue
import javax.imageio.ImageIO
import hexagon.resource.ResourceWrapper
import hexagon.resource.TextureArray

object BlockLoader {
  private var texIdxMap: Map[String, Int] = _
  
  def init(): Unit = {
    TextureArray.registerTextureArray("blocks", BlockTexture.blockTextureSize, new ResourceWrapper(loadAllBlockTextures))
  }
  
  def loadAllBlockTextures(): Seq[Array[Int]] = {
    val nameToIdx = collection.mutable.Map.empty[String, Int]
    val images = collection.mutable.ArrayBuffer.empty[Array[Int]]
    
    def loadImages(file: File): Seq[Array[Int]] = {
      val image = ImageIO.read(file)
      val w = image.getWidth
      val h = image.getHeight
      val numImages = w / h
      for (i <- 0 until numImages) yield image.getRGB(i * h, 0, h, h, null, 0, h)
    }
    
    val dir = new File("res/textures/blocks")
    for (f <- dir.listFiles()) {
      if (f.isFile()) {
        val lastDot = f.getName.lastIndexOf('.')
        val name = f.getName.substring(0, lastDot)
        nameToIdx += name -> images.size
        images ++= loadImages(f)
      }
    }
    
    texIdxMap = nameToIdx.toMap
    images
  }
  
  def loadBlockType(name: String): IndexedSeq[Int] = {
    val file = new File("res/spec/blocks/" + name + ".json")
    if (file.isFile) {
      val base = Json.parse(new FileReader(file)).asObject()
      val textures = base.get("textures").asObject()
      val all = textures.get("all")
      val side = textures.get("side") or all
      val topIdx = texIdxMap((textures.get("top") or all).asString())
      val bottomIdx = texIdxMap((textures.get("bottom") or all).asString())
      val sidesIdx = (2 until 8).map(i => texIdxMap((textures.get(s"side$i") or side).asString()))
      topIdx +: bottomIdx +: sidesIdx
    } else IndexedSeq.fill(8)(0)
  }
  
  implicit class DefaultJsonImplicitClass(left: JsonValue) {
    def or(right: JsonValue): JsonValue = if (left == null) right else left
  }
}

package hexacraft.world

import com.martomate.nbt.Nbt
import hexacraft.world.settings.{WorldGenSettings, WorldInfo}

class FakeWorldProvider(seed: Long)(using cylSize: CylinderSize) extends WorldProvider {
  override def getWorldInfo: WorldInfo = new WorldInfo(
    "test world",
    cylSize,
    new WorldGenSettings(seed, 0.1, 0.01, 0.01, 0.001, 0.001),
    Nbt.emptyMap
  )

  private var fs: Map[String, Nbt.MapTag] = Map.empty

  override def loadState(path: String): Nbt.MapTag = fs.getOrElse(path, Nbt.emptyMap)

  override def saveState(tag: Nbt.MapTag, name: String, path: String): Unit = fs += path -> tag
}

package hexacraft.world

import com.martomate.nbt.Nbt

class FakeWorldProvider(seed: Long)(using cylSize: CylinderSize) extends WorldProvider {
  private var playerNbt = Nbt.emptyMap

  def setPlayer(nbt: Nbt.MapTag): Unit = {
    playerNbt = nbt
  }

  override def getWorldInfo: WorldInfo = new WorldInfo(
    "test world",
    cylSize,
    new WorldGenSettings(seed, 0.1, 0.01, 0.01, 0.001, 0.001),
    playerNbt
  )

  private var fs: Map[String, Nbt.MapTag] = Map.empty

  override def loadState(path: String): Option[Nbt.MapTag] = fs.get(path)

  override def saveState(tag: Nbt.MapTag, name: String, path: String): Unit = fs += path -> tag
}

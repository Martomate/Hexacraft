package hexacraft.world

import hexacraft.nbt.Nbt

class FakeWorldProvider(seed: Long)(using cylSize: CylinderSize) extends WorldProvider {
  override def getWorldInfo: WorldInfo = new WorldInfo(
    1,
    "test world",
    cylSize,
    new WorldGenSettings(seed, 0.1, 0.01, 0.01, 0.001, 0.001)
  )

  private var fs: Map[WorldProvider.Path, Nbt.MapTag] = Map.empty

  override def loadState(path: WorldProvider.Path): Option[Nbt.MapTag] = fs.get(path)

  override def saveState(path: WorldProvider.Path, tag: Nbt.MapTag): Unit = fs += path -> tag
}

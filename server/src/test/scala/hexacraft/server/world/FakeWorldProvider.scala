package hexacraft.server.world

import hexacraft.nbt.Nbt
import hexacraft.world.{CylinderSize, WorldGenSettings, WorldInfo}

class FakeWorldProvider(seed: Long, generateOceans: Boolean = false)(using cylSize: CylinderSize)
    extends WorldProvider {
  val worldInfo = new WorldInfo(
    1,
    "test world",
    cylSize,
    new WorldGenSettings(seed, 0.1, 0.01, 0.01, 0.001, 0.001, generateOceans)
  )

  private var fs: Map[WorldProvider.Path, Nbt.MapTag] = Map.empty

  override def loadState(path: WorldProvider.Path): Option[Nbt.MapTag] = fs.get(path)

  override def saveState(path: WorldProvider.Path, tag: Nbt.MapTag): Unit = fs += path -> tag
}

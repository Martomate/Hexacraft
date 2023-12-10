package hexacraft.world.settings

import hexacraft.world.{CylinderSize, MigrationManager}

import com.martomate.nbt.Nbt

import java.io.File

case class WorldInfo(worldName: String, worldSize: CylinderSize, gen: WorldGenSettings, player: Nbt.MapTag) {
  def toNBT: Nbt.MapTag = {
    Nbt.makeMap(
      "version" -> Nbt.ShortTag(MigrationManager.LatestVersion),
      "general" -> Nbt.makeMap(
        "worldSize" -> Nbt.ByteTag(worldSize.worldSize.toByte),
        "name" -> Nbt.StringTag(worldName)
      ),
      "gen" -> gen.toNBT,
      "player" -> player
    )
  }
}

object WorldInfo {
  def fromNBT(nbtData: Nbt.MapTag, saveDir: File, worldSettings: WorldSettings): WorldInfo = {
    val generalSettings = nbtData.getMap("general").getOrElse(Nbt.emptyMap)

    val name = generalSettings.getString("name", worldSettings.name.getOrElse(saveDir.getName))
    val worldSize = generalSettings.getByte("worldSize", worldSettings.size.getOrElse(7))
    val gen = nbtData.getMap("gen").getOrElse(Nbt.emptyMap)
    val player = nbtData.getMap("player").orNull

    WorldInfo(
      worldName = name,
      worldSize = CylinderSize(worldSize),
      gen = WorldGenSettings.fromNBT(gen, worldSettings),
      player = player
    )
  }
}

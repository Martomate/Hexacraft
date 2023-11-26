package hexacraft.world.settings

import com.martomate.nbt.{Nbt, NBTUtil}
import hexacraft.world.{CylinderSize, MigrationManager}

import java.io.File

class WorldInfo(
    val worldName: String,
    val worldSize: CylinderSize,
    val gen: WorldGenSettings,
    val player: Nbt.MapTag
) {
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
    val generalSettings: Nbt.MapTag =
      nbtData.getCompoundTag("general").getOrElse(Nbt.emptyMap)
    val name: String =
      generalSettings.getString("name", worldSettings.name.getOrElse(saveDir.getName))
    val size: CylinderSize = CylinderSize(
      generalSettings.getByte("worldSize", worldSettings.size.getOrElse(7))
    )
    val gen: WorldGenSettings =
      WorldGenSettings.fromNBT(nbtData.getCompoundTag("gen").getOrElse(Nbt.emptyMap), worldSettings)
    val player: Nbt.MapTag = nbtData.getCompoundTag("player").orNull

    new WorldInfo(name, size, gen, player)
  }
}

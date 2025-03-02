package hexacraft.world

import com.martomate.nbt.Nbt

import java.io.File
import java.util.Random

case class WorldInfo(private val version: Short, worldName: String, worldSize: CylinderSize, gen: WorldGenSettings) {
  def toNbt: Nbt.MapTag = {
    Nbt.makeMap(
      "version" -> Nbt.ShortTag(version),
      "general" -> Nbt.makeMap(
        "worldSize" -> Nbt.ByteTag(worldSize.worldSize.toByte),
        "name" -> Nbt.StringTag(worldName)
      ),
      "gen" -> gen.toNBT
    )
  }
}

object WorldInfo {
  def fromNBT(nbtData: Nbt.MapTag, saveDir: File, worldSettings: WorldSettings): WorldInfo = {
    val generalSettings = nbtData.getMap("general").getOrElse(Nbt.emptyMap)

    val name = generalSettings.getString("name", worldSettings.name.getOrElse(saveDir.getName))
    val worldSize = generalSettings.getByte("worldSize", worldSettings.size.getOrElse(7))
    val gen = nbtData.getMap("gen").getOrElse(Nbt.emptyMap)

    WorldInfo(
      version = nbtData.getShort("version", 0.toShort),
      worldName = name,
      worldSize = CylinderSize(worldSize),
      gen = WorldGenSettings.fromNBT(gen, worldSettings)
    )
  }
}

class WorldGenSettings(
    val seed: Long,
    val blockGenScale: Double,
    val heightMapGenScale: Double,
    val blockDensityGenScale: Double,
    val biomeHeightMapGenScale: Double,
    val biomeHeightVariationGenScale: Double
) {

  def toNBT: Nbt.MapTag = Nbt.makeMap(
    "seed" -> Nbt.LongTag(seed),
    "blockGenScale" -> Nbt.DoubleTag(blockGenScale),
    "heightMapGenScale" -> Nbt.DoubleTag(heightMapGenScale),
    "blockDensityGenScale" -> Nbt.DoubleTag(blockDensityGenScale),
    "biomeHeightGenScale" -> Nbt.DoubleTag(biomeHeightMapGenScale),
    "biomeHeightVariationGenScale" -> Nbt.DoubleTag(biomeHeightVariationGenScale)
  )
}

object WorldGenSettings {
  def fromNBT(nbt: Nbt.MapTag, defaultSettings: WorldSettings): WorldGenSettings = {
    new WorldGenSettings(
      nbt.getLong("seed", defaultSettings.seed.getOrElse(new Random().nextLong)),
      nbt.getDouble("blockGenScale", 0.1),
      nbt.getDouble("heightMapGenScale", 0.02),
      nbt.getDouble("blockDensityGenScale", 0.01),
      nbt.getDouble("biomeHeightMapGenScale", 0.002),
      nbt.getDouble("biomeHeightVariationGenScale", 0.002)
    )
  }
}

case class WorldSettings(name: Option[String], size: Option[Byte], seed: Option[Long])

object WorldSettings {
  def none: WorldSettings = WorldSettings(None, None, None)
}

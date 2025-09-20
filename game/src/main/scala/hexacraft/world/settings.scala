package hexacraft.world

import hexacraft.nbt.{Nbt, NbtEncoder}

import java.io.File
import java.util.Random

case class WorldInfo(private val version: Short, worldName: String, worldSize: CylinderSize, gen: WorldGenSettings)

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

  given NbtEncoder[WorldInfo] with {
    override def encode(info: WorldInfo): Nbt.MapTag = {
      Nbt.makeMap(
        "version" -> Nbt.ShortTag(info.version),
        "general" -> Nbt.makeMap(
          "worldSize" -> Nbt.ByteTag(info.worldSize.worldSize.toByte),
          "name" -> Nbt.StringTag(info.worldName)
        ),
        "gen" -> Nbt.encode(info.gen)
      )
    }
  }
}

class WorldGenSettings(
    val seed: Long,
    val blockGenScale: Double,
    val heightMapGenScale: Double,
    val blockDensityGenScale: Double,
    val biomeHeightMapGenScale: Double,
    val biomeHeightVariationGenScale: Double,
    val humidityGenScale: Double,
    val temperatureGenScale: Double
)

object WorldGenSettings {
  def fromNBT(nbt: Nbt.MapTag, defaultSettings: WorldSettings): WorldGenSettings = {
    new WorldGenSettings(
      nbt.getLong("seed", defaultSettings.seed.getOrElse(new Random().nextLong)),
      nbt.getDouble("blockGenScale", 0.1),
      nbt.getDouble("heightMapGenScale", 0.02),
      nbt.getDouble("blockDensityGenScale", 0.01),
      nbt.getDouble("biomeHeightMapGenScale", 0.002),
      nbt.getDouble("biomeHeightVariationGenScale", 0.002),
      nbt.getDouble("humidityGenScale", 0.002),
      nbt.getDouble("temperatureGenScale", 0.002)
    )
  }

  given NbtEncoder[WorldGenSettings] with {
    override def encode(s: WorldGenSettings): Nbt.MapTag = {
      Nbt.makeMap(
        "seed" -> Nbt.LongTag(s.seed),
        "blockGenScale" -> Nbt.DoubleTag(s.blockGenScale),
        "heightMapGenScale" -> Nbt.DoubleTag(s.heightMapGenScale),
        "blockDensityGenScale" -> Nbt.DoubleTag(s.blockDensityGenScale),
        "biomeHeightGenScale" -> Nbt.DoubleTag(s.biomeHeightMapGenScale),
        "biomeHeightVariationGenScale" -> Nbt.DoubleTag(s.biomeHeightVariationGenScale),
        "humidityGenScale" -> Nbt.DoubleTag(s.humidityGenScale),
        "temperatureGenScale" -> Nbt.DoubleTag(s.temperatureGenScale)
      )
    }
  }
}

case class WorldSettings(name: Option[String], size: Option[Byte], seed: Option[Long])

object WorldSettings {
  def none: WorldSettings = WorldSettings(None, None, None)
}

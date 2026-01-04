package hexacraft.world

import hexacraft.nbt.{Nbt, NbtDecoder, NbtEncoder}

case class WorldInfo(private val version: Short, worldName: String, worldSize: CylinderSize, gen: WorldGenSettings)

object WorldInfo {
  given NbtDecoder[WorldInfo] with {
    override def decode(nbtData: Nbt.MapTag): Option[WorldInfo] = {
      val generalSettings = nbtData.getMap("general").getOrElse(Nbt.emptyMap)

      val worldInfo = WorldInfo(
        version = nbtData.getShort("version", 0.toShort),
        worldName = generalSettings.getString("name", "World"),
        worldSize = CylinderSize(generalSettings.getByte("worldSize", 7)),
        gen = Nbt.decode[WorldGenSettings](nbtData.getMap("gen").getOrElse(Nbt.emptyMap)).get
      )
      Some(worldInfo)
    }
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
    val biomeHeightVariationGenScale: Double
)

object WorldGenSettings {
  def fromSeed(seed: Long): WorldGenSettings = {
    Nbt.decode(Nbt.makeMap("seed" -> Nbt.LongTag(seed))).get
  }

  given NbtDecoder[WorldGenSettings] with {
    override def decode(nbt: Nbt.MapTag): Option[WorldGenSettings] = {
      val settings = new WorldGenSettings(
        nbt.getLong("seed", 0),
        nbt.getDouble("blockGenScale", 0.1),
        nbt.getDouble("heightMapGenScale", 0.02),
        nbt.getDouble("blockDensityGenScale", 0.01),
        nbt.getDouble("biomeHeightMapGenScale", 0.002),
        nbt.getDouble("biomeHeightVariationGenScale", 0.002)
      )
      Some(settings)
    }
  }

  given NbtEncoder[WorldGenSettings] with {
    override def encode(s: WorldGenSettings): Nbt.MapTag = {
      Nbt.makeMap(
        "seed" -> Nbt.LongTag(s.seed),
        "blockGenScale" -> Nbt.DoubleTag(s.blockGenScale),
        "heightMapGenScale" -> Nbt.DoubleTag(s.heightMapGenScale),
        "blockDensityGenScale" -> Nbt.DoubleTag(s.blockDensityGenScale),
        "biomeHeightGenScale" -> Nbt.DoubleTag(s.biomeHeightMapGenScale),
        "biomeHeightVariationGenScale" -> Nbt.DoubleTag(s.biomeHeightVariationGenScale)
      )
    }
  }
}

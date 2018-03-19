package hexacraft.world.storage

import java.io.{File, FileInputStream}
import java.util.Random

import com.flowpowered.nbt.{CompoundMap, CompoundTag, DoubleTag, LongTag}
import com.flowpowered.nbt.stream.NBTInputStream
import hexacraft.util.NBTUtil
import hexacraft.world.WorldSettings

class WorldGenSettings(val nbt: CompoundTag, val defaultSettings: WorldSettings) {
  val seed                        : Long   = NBTUtil.getLong  (nbt, "seed", defaultSettings.seed.getOrElse(new Random().nextLong))
  val blockGenScale               : Double = NBTUtil.getDouble(nbt, "blockGenScale", 0.1)
  val heightMapGenScale           : Double = NBTUtil.getDouble(nbt, "heightMapGenScale", 0.02)
  val blockDensityGenScale        : Double = NBTUtil.getDouble(nbt, "blockDensityGenScale", 0.01)
  val biomeHeightMapGenScale      : Double = NBTUtil.getDouble(nbt, "biomeHeightMapGenScale", 0.002)
  val biomeHeightVariationGenScale: Double = NBTUtil.getDouble(nbt, "biomeHeightVariationGenScale", 0.002)

  def toNBT: CompoundTag = NBTUtil.makeCompoundTag("gen", Seq(
    new LongTag("seed", seed),
    new DoubleTag("blockGenScale", blockGenScale),
    new DoubleTag("heightMapGenScale", heightMapGenScale),
    new DoubleTag("blockDensityGenScale", blockDensityGenScale),
    new DoubleTag("biomeHeightGenScale", biomeHeightMapGenScale),
    new DoubleTag("biomeHeightVariationGenScale", biomeHeightVariationGenScale)
  ))
}

trait WorldSettingsProvider {
  def name: String
  def size: CylinderSize
  def gen: WorldGenSettings
  def playerNBT: CompoundTag

  def loadState(path: String): CompoundTag
  def saveState(tag: CompoundTag, path: String = "world.dat"): Unit
}

class WorldSettingsProviderFromFile(saveDir: File, worldSettings: WorldSettings) extends WorldSettingsProvider {
  private val nbtData: CompoundTag = loadState("world.dat")
  private val generalSettings: CompoundTag = nbtData.getValue.get("general").asInstanceOf[CompoundTag]

  val name: String = NBTUtil.getString(generalSettings, "worldName", worldSettings.name.getOrElse(saveDir.getName))
  val size: CylinderSize = new CylinderSize(NBTUtil.getByte(generalSettings, "worldSize", worldSettings.size.getOrElse(7)))
  def gen: WorldGenSettings = new WorldGenSettings(nbtData.getValue.get("gen").asInstanceOf[CompoundTag], worldSettings)
  def playerNBT: CompoundTag = nbtData.getValue.get("player").asInstanceOf[CompoundTag]

  def loadState(path: String): CompoundTag = {
    val file = new File(saveDir, path)
    if (file.isFile) {
      val stream = new NBTInputStream(new FileInputStream(file))
      val nbt = stream.readTag().asInstanceOf[CompoundTag]
      stream.close()
      nbt
    } else {
      new CompoundTag("", new CompoundMap())
    }
  }

  def saveState(tag: CompoundTag, path: String = "world.dat"): Unit = {
    NBTUtil.saveTag(tag, new File(saveDir, path))
  }
}

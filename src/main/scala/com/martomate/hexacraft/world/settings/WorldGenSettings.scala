package com.martomate.hexacraft.world.settings

import com.martomate.hexacraft.util.NBTUtil

import com.flowpowered.nbt.{CompoundTag, DoubleTag, IntTag, LongTag, Tag}
import java.util.Random

sealed trait WorldGenVersion:
  def version: Int
  def toNBT: Seq[Tag[_]]

case class WorldGenV1(
    blockGenScale: Double,
    heightMapGenScale: Double,
    blockDensityGenScale: Double,
    biomeHeightMapGenScale: Double,
    biomeHeightVariationGenScale: Double
) extends WorldGenVersion:
  override def version: Int = 1

  override def toNBT: Seq[Tag[_]] = Seq(
    new DoubleTag("blockGenScale", blockGenScale),
    new DoubleTag("heightMapGenScale", heightMapGenScale),
    new DoubleTag("blockDensityGenScale", blockDensityGenScale),
    new DoubleTag("biomeHeightGenScale", biomeHeightMapGenScale),
    new DoubleTag("biomeHeightVariationGenScale", biomeHeightVariationGenScale)
  )

object WorldGenV1:
  def fromNBT(nbt: CompoundTag): WorldGenV1 =
    WorldGenV1(
      NBTUtil.getDouble(nbt, "blockGenScale", 0.1),
      NBTUtil.getDouble(nbt, "heightMapGenScale", 0.02),
      NBTUtil.getDouble(nbt, "blockDensityGenScale", 0.01),
      NBTUtil.getDouble(nbt, "biomeHeightMapGenScale", 0.002),
      NBTUtil.getDouble(nbt, "biomeHeightVariationGenScale", 0.002)
    )

class WorldGenSettings(
    val seed: Long,
    val versionedSettings: WorldGenVersion
):
  def toNBT: CompoundTag =
    NBTUtil.makeCompoundTag(
      "gen",
      Seq(
        new LongTag("seed", seed),
        new IntTag("version", versionedSettings.version)
      ) ++ versionedSettings.toNBT
    )

object WorldGenSettings:
  def fromNBT(nbt: CompoundTag, defaultSettings: WorldSettings): WorldGenSettings =
    val seed = NBTUtil.getLong(nbt, "seed", defaultSettings.seed.getOrElse(new Random().nextLong))
    val version = NBTUtil.getInt(nbt, "version", 1)

    val extraSettings = version match
      case 1 => WorldGenV1.fromNBT(nbt)

    new WorldGenSettings(seed, extraSettings)

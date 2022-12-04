package com.martomate.hexacraft.world.gen

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.block.{Blocks, BlockState}
import com.martomate.hexacraft.world.chunk.storage.{ChunkStorage, DenseChunkStorage}
import com.martomate.hexacraft.world.coord.fp.BlockCoords
import com.martomate.hexacraft.world.coord.integer.{BlockRelChunk, ChunkRelWorld, ColumnRelWorld}
import com.martomate.hexacraft.world.gen.v1.WorldGeneratorV1
import com.martomate.hexacraft.world.settings.{WorldGenSettings, WorldGenV1}

import com.flowpowered.nbt.CompoundTag
import java.util.Random

class WorldGenerator(settings: WorldGenSettings)(using CylinderSize, Blocks) {
  private val generator =
    settings.versionedSettings match
      case s: WorldGenV1 => new WorldGeneratorV1(settings.seed, s)

  def heightMap(coords: ColumnRelWorld): IndexedSeq[IndexedSeq[Short]] =
    generator.heightMap(coords)

  def blocksInChunk(coords: ChunkRelWorld, heightMap: IndexedSeq[IndexedSeq[Short]]): ChunkStorage =
    generator.blocksInChunk(coords, heightMap)

  def toNBT: CompoundTag = settings.toNBT
}

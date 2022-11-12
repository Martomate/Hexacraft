package com.martomate.hexacraft.world.gen

import com.flowpowered.nbt.Tag
import com.martomate.hexacraft.world.chunk.ChunkColumn
import com.martomate.hexacraft.world.chunk.storage.ChunkStorage
import com.martomate.hexacraft.world.coord.integer.{ChunkRelWorld, ColumnRelWorld}

trait WorldGeneratorStrategy:
  type HeightMap = IndexedSeq[IndexedSeq[Short]]

  def heightMap(coords: ColumnRelWorld): HeightMap

  def blocksInChunk(coords: ChunkRelWorld, heightMap: HeightMap): ChunkStorage

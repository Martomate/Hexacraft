package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.block.BlockState
import com.martomate.hexacraft.world.chunk.BlockInChunkAccessor
import com.martomate.hexacraft.world.coord.integer.{BlockRelChunk, BlockRelWorld, ChunkRelWorld, NeighborOffsets}

import scala.collection.mutable

class ChunkOpaqueDeterminer(chunkCoords: ChunkRelWorld, chunk: BlockInChunkAccessor)(using
    CylinderSize
):
  private val sideGroups: Array[mutable.Set[Int]] = Array.fill(8)(mutable.Set.empty)
  private var sideGroupValid: Boolean = false

  def canGetToSide(fromSide: Int, toSide: Int): Boolean =
    if !sideGroupValid then
      sideGroupValid = true
      calculateSideGroups()

    (sideGroups(fromSide) & sideGroups(toSide)).nonEmpty

  def invalidate(): Unit = sideGroupValid = false

  private def calculateSideGroups(): Unit =
    sideGroups.foreach(_.clear())

    val chunkLookup: Map[ChunkRelWorld, Int] =
      NeighborOffsets.all.map(t => chunkCoords.offset(t)).zipWithIndex.toMap

    val group = new Array[Int](16 * 16 * 16)
    val bfs = mutable.Queue.empty[Int]
    var currentGroup = 1

    for startIdx <- 0 until 16 * 16 * 16 do
      if group(startIdx) == 0 then
        bfs += startIdx

        while bfs.nonEmpty do
          val idx = bfs.dequeue()

          if group(idx) == 0 then
            val c = BlockRelChunk(idx)
            val solid = chunk.getBlock(c) != BlockState.Air

            group(idx) = if solid then -1 else currentGroup

            if !solid then
              for s <- NeighborOffsets.indices do
                if c.onChunkEdge(s)
                then
                  val ch = BlockRelWorld
                    .fromChunk(c, chunkCoords)
                    .offset(NeighborOffsets(s))
                    .getChunkRelWorld
                  sideGroups(chunkLookup(ch)) += currentGroup
                else
                  val n = c.neighbor(s).value
                  if group(n) == 0 then bfs += n

        currentGroup += 1

package com.martomate.hexacraft.world

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.util.MathUtils.oppositeSide
import com.martomate.hexacraft.world.block.BlockState
import com.martomate.hexacraft.world.chunk.Chunk
import com.martomate.hexacraft.world.chunk.storage.LocalBlockState
import com.martomate.hexacraft.world.coord.integer.{BlockRelChunk, BlockRelWorld, NeighborOffsets}

import scala.collection.mutable

class LightPropagator(world: BlocksInWorld)(implicit cylSize: CylinderSize) {
  private val chunkCache: ChunkCache = new ChunkCache(world)

  def initBrightnesses(chunk: Chunk): Unit = {
    chunkCache.clearCache()

    val lights = mutable.HashMap.empty[BlockRelChunk, BlockState]

    for
      LocalBlockState(c, b) <- chunk.blocks
      if b.blockType.lightEmitted != 0
    do lights(c) = b

    val queueTorch = mutable.Queue.empty[(BlockRelChunk, Chunk)]
    val queueSun15 = mutable.Queue.empty[(BlockRelChunk, Chunk)]
    val queueSunFromNeighbor = mutable.Queue.empty[(BlockRelChunk, Chunk)]

    for ((c, b) <- lights) {
      chunk.lighting.setTorchlight(c, b.blockType.lightEmitted)
      queueTorch += ((c, chunk))
    }

    def shouldEnqueueSunlight(coords: BlockRelChunk, neigh: Chunk) = {
      val block = neigh.getBlock(coords)
      val neighCol = world.getColumn(neigh.coords.getColumnRelWorld).get
      if neigh.coords.Y * 16 + coords.cy > neighCol.terrainHeight(coords.cx, coords.cz)
      then
        val transparentTop = block.blockType.isTransparent(block.metadata, 0)
        val transparentBottom = block.blockType.isTransparent(block.metadata, 1)
        transparentTop && transparentBottom
      else false
    }

    def handleEdge(x: Int, y: Int, z: Int) = {
      val c2 = BlockRelWorld(x, y, z, chunk.coords)
      val c2c = c2.getBlockRelChunk
      val isAboveChunk = y == 16 && (x & ~15 | z & ~15) == 0

      chunkCache.getChunk(c2.getChunkRelWorld) match
        case null =>
          if isAboveChunk && shouldEnqueueSunlight(c2c.offset(0, -1, 0), chunk)
          then
            chunk.lighting.setSunlight(c2c.offset(0, -1, 0), 15)
            queueSun15 += ((c2c.offset(0, -1, 0), chunk))
        case neigh =>
          if neigh.lighting.getTorchlight(c2c) > 1
          then queueTorch += ((c2c, neigh))

          if isAboveChunk && shouldEnqueueSunlight(c2c, neigh)
          then
            neigh.lighting.setSunlight(c2c, 15)
            queueSun15 += ((c2c, neigh))

          val lightHere = neigh.lighting.getSunlight(c2c)
          if lightHere > 0 && lightHere < 15
          then queueSunFromNeighbor += ((c2c, neigh))
    }

    for (s <- -1 to 16) {
      for (t <- -1 to 16) {
        handleEdge(-1, s, t)
        handleEdge(16, s, t)
        handleEdge(s, -1, t)
        handleEdge(s, 16, t)
        handleEdge(s, t, -1)
        handleEdge(s, t, 16)
      }
    }

    propagateTorchlight(queueTorch)
    propagateSunlight(queueSun15)
    propagateSunlight(queueSunFromNeighbor)
  }

  def addTorchlight(chunk: Chunk, coords: BlockRelChunk, value: Int): Unit = {
    chunkCache.clearCache()
    chunk.lighting.setTorchlight(coords, value)
    val queue = mutable.Queue(coords -> chunk)
    propagateTorchlight(queue)
  }

  def removeTorchlight(chunk: Chunk, coords: BlockRelChunk): Unit = {
    chunkCache.clearCache()
    val currentLight = chunk.lighting.getTorchlight(coords)
    chunk.lighting.setTorchlight(coords, 0)
    propagateTorchlightRemoval(mutable.Queue((coords, chunk, currentLight)))
  }

  def removeSunlight(chunk: Chunk, coords: BlockRelChunk): Unit = {
    chunkCache.clearCache()
    val currentLight = chunk.lighting.getSunlight(coords)
    chunk.lighting.setSunlight(coords, 0)
    propagateSunlightRemoval(mutable.Queue((coords, chunk, currentLight)))
  }

  private def propagateTorchlightRemoval(
      queue: mutable.Queue[(BlockRelChunk, Chunk, Int)]
  ): Unit = {
    val lightQueue = mutable.Queue.empty[(BlockRelChunk, Chunk)]

    val chunksNeedingRenderUpdate = mutable.HashSet.empty[Chunk]

    while (queue.nonEmpty) {
      val (here, chunk, level) = queue.dequeue()

      chunksNeedingRenderUpdate += chunk

      for (s <- NeighborOffsets.indices) {
        val c2w = here.globalNeighbor(s, chunk.coords)
        val c2 = c2w.getBlockRelChunk
        val neigh = chunkCache.getChunk(c2w.getChunkRelWorld)
        if (neigh != null) {
          val thisLevel = neigh.lighting.getTorchlight(c2)
          if (thisLevel != 0) {
            if (thisLevel < level) {
              queue += ((c2, neigh, thisLevel))
              neigh.lighting.setTorchlight(c2, 0)
            } else {
              lightQueue += ((c2, neigh))
            }
          } else
            chunksNeedingRenderUpdate += neigh // the if-case above gets handled later since it's in the queue
        }
      }
    }

    chunksNeedingRenderUpdate.foreach(_.requestRenderUpdate())

    propagateTorchlight(lightQueue)
  }

  private def propagateSunlightRemoval(queue: mutable.Queue[(BlockRelChunk, Chunk, Int)]): Unit = {
    val lightQueue = mutable.Queue.empty[(BlockRelChunk, Chunk)]

    val chunksNeedingRenderUpdate = mutable.HashSet.empty[Chunk]

    while (queue.nonEmpty) {
      val (here, chunk, level) = queue.dequeue()

      chunksNeedingRenderUpdate += chunk

      for (s <- NeighborOffsets.indices) {
        val c2w = here.globalNeighbor(s, chunk.coords)
        val c2 = c2w.getBlockRelChunk
        val neigh = chunkCache.getChunk(c2w.getChunkRelWorld)
        if (neigh != null) {
          val thisLevel = neigh.lighting.getSunlight(c2)
          if (thisLevel != 0) {
            if (thisLevel < level || (s == 1 && level == 15)) {
              queue += ((c2, neigh, thisLevel))
              neigh.lighting.setSunlight(c2, 0)
            } else {
              lightQueue += ((c2, neigh))
            }
          } else
            chunksNeedingRenderUpdate += neigh // the if-case above gets handled later since it's in the queue
        }
      }
    }

    chunksNeedingRenderUpdate.foreach(_.requestRenderUpdate())
    propagateSunlight(lightQueue)
  }

  private def propagateTorchlight(queue: mutable.Queue[(BlockRelChunk, Chunk)]): Unit = {
    val chunksNeedingRenderUpdate = mutable.HashSet.empty[Chunk]

    while (queue.nonEmpty) {
      val (here, chunk) = queue.dequeue()

      val nextLevel = chunk.lighting.getTorchlight(here) - 1

      if (nextLevel > 0) {
        chunksNeedingRenderUpdate += chunk

        for (s <- NeighborOffsets.indices) {
          val c2w = here.globalNeighbor(s, chunk.coords)
          val c2 = c2w.getBlockRelChunk
          val neigh = chunkCache.getChunk(c2w.getChunkRelWorld)
          if (neigh != null) {
            val block = neigh.getBlock(c2)
            if (block.blockType.isTransparent(block.metadata, oppositeSide(s))) {
              val thisTLevel = neigh.lighting.getTorchlight(c2)
              if (thisTLevel < nextLevel) {
                neigh.lighting.setTorchlight(c2, nextLevel)
                queue += ((c2, neigh))
              }
            } else
              chunksNeedingRenderUpdate += neigh // the if-case above gets handled later since it's in the queue
          }
        }
      }
    }

    chunksNeedingRenderUpdate.foreach(_.requestRenderUpdate())
  }

  private def propagateSunlight(queue: mutable.Queue[(BlockRelChunk, Chunk)]): Unit = {
    val chunksNeedingRenderUpdate = mutable.HashSet.empty[Chunk]

    if (queue.nonEmpty) {
      val chunksToProcess = mutable.Map.empty[Chunk, mutable.Queue[Int]]
      while (queue.nonEmpty) {
        val (here, chunk) = queue.dequeue()
        chunksToProcess.getOrElseUpdate(chunk, mutable.Queue.empty).enqueue(here.value)
        chunksNeedingRenderUpdate += chunk
      }
      while (chunksToProcess.nonEmpty) {
        val chunk = chunksToProcess.head._1
        val thisQueue = chunksToProcess.remove(chunk).get
        val map = propagateSunlightInChunk(chunk, thisQueue)
        for (ch <- map.keysIterator) {
          chunksToProcess.getOrElseUpdate(ch, mutable.Queue.empty).enqueueAll(map(ch))
          chunksNeedingRenderUpdate += ch
        }
      }
    }

    chunksNeedingRenderUpdate.foreach(_.requestRenderUpdate())
  }

  private def propagateSunlightInChunk(
      chunk: Chunk,
      queue: mutable.Queue[Int]
  ): Map[Chunk, Seq[Int]] = {
    val neighborMap: mutable.Map[Chunk, mutable.ArrayBuffer[Int]] = mutable.Map.empty
    val inQueue: java.util.BitSet = new java.util.BitSet(16 * 16 * 16)

    while (queue.nonEmpty) {
      val hereValue = queue.dequeue()
      val here = BlockRelChunk(hereValue)
      inQueue.clear(hereValue)

      val nextLevel = chunk.lighting.getSunlight(here) - 1

      if (nextLevel > 0) {
        var s = 0
        while (s < 8) { // done as an optimization
          if (here.isOnChunkEdge(s)) {
            val c2w = here.globalNeighbor(s, chunk.coords)
            val crw = c2w.getChunkRelWorld
            val c2 = c2w.getBlockRelChunk
            val neigh = chunkCache.getChunk(crw)
            if (neigh != null) {
              val otherSide = oppositeSide(s)

              val bl = neigh.getBlock(c2)
              if (bl.blockType.isTransparent(bl.metadata, otherSide)) {
                val thisSLevel = neigh.lighting.getSunlight(c2)
                val nextS = if (nextLevel == 14 && s == 1) nextLevel + 1 else nextLevel

                if (!neighborMap.contains(neigh)) neighborMap.put(neigh, mutable.ArrayBuffer.empty)

                if (thisSLevel < nextS) {
                  neigh.lighting.setSunlight(c2, nextS)
                  neighborMap(neigh).append(c2.value)
                }
              }
            }
          } else {
            val c2 = here.neighbor(s)
            val otherSide = oppositeSide(s)

            val bl = chunk.getBlock(c2)
            if (bl.blockType.isTransparent(bl.metadata, otherSide)) {
              val thisSLevel = chunk.lighting.getSunlight(c2)
              val nextS = if (nextLevel == 14 && s == 1) nextLevel + 1 else nextLevel
              if (thisSLevel < nextS) {
                chunk.lighting.setSunlight(c2, nextS)

                if (!inQueue.get(c2.value)) {
                  inQueue.set(c2.value)
                  queue += c2.value
                }
              }
            }
          }

          s += 1
        }
      }
    }

    neighborMap.view.mapValues(_.toSeq).toMap
  }
}

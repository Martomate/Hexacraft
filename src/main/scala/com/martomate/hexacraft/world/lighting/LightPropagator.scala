package com.martomate.hexacraft.world.lighting

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.{BlocksInWorld, ChunkCache}
import com.martomate.hexacraft.world.block.state.BlockState
import com.martomate.hexacraft.world.chunk.Chunk
import com.martomate.hexacraft.world.coord.integer.{BlockRelChunk, BlockRelWorld, NeighborOffsets}

import scala.collection.mutable

class LightPropagator(world: BlocksInWorld)(implicit cylSize: CylinderSize) {
  private val chunkCache: ChunkCache = new ChunkCache(world)

  def initBrightnesses(chunk: Chunk, lights: Map[BlockRelChunk, BlockState]): Unit = {
    chunkCache.clearCache()
    val queueTorch = mutable.Queue.empty[(BlockRelChunk, Chunk)]
    val queueSun15 = mutable.Queue.empty[(BlockRelChunk, Chunk)]
    val queueSunFromNeighbor = mutable.Queue.empty[(BlockRelChunk, Chunk)]
    for ((c, b) <- lights) {
      chunk.lighting.setTorchlight(c, b.blockType.lightEmitted)
      queueTorch += ((c, chunk))
    }

    def enqueueIfTransparent(coords: BlockRelChunk, neigh: Chunk): Unit = {
      val block = neigh.getBlock(coords)
      val neighCol = world.getColumn(neigh.coords.getColumnRelWorld).get
      if (neigh.coords.Y * 16 + coords.cy > neighCol.heightMap(coords.cx, coords.cz)) {
        if (block.blockType.isTransparent(block.metadata, 0) && block.blockType.isTransparent(block.metadata, 1)) {
          neigh.lighting.setSunlight(coords, 15)
          queueSun15 += ((coords, neigh))
        }
      }
    }

    for (y <- -1 to 16) {
      for (z <- -1 to 16) {
        Seq(
          (-1, y, z),
          (16, y, z),
          (y, -1, z),
          (y, 16, z),
          (y, z, -1),
          (y, z, 16)
        ) foreach { t =>
          val c2 = BlockRelWorld(t._1, t._2, t._3, chunk.coords)
          val c2c = c2.getBlockRelChunk
          Option(chunkCache.getChunk(c2.getChunkRelWorld)) match {
            case Some(neigh) =>
              if (neigh.lighting.getTorchlight(c2c) > 1) {
                queueTorch += ((c2c, neigh))
              }
              if (t._2 == 16 && (t._1 & ~15 | t._3 & ~15) == 0) {
                enqueueIfTransparent(c2c, neigh)
              }
              val lightHere = neigh.lighting.getSunlight(c2c)
              if (lightHere > 0 && lightHere < 15) {
                queueSunFromNeighbor += ((c2c, neigh))
              }
            case _ =>
              if (t._2 == 16 && (t._1 & ~15 | t._3 & ~15) == 0) {
                enqueueIfTransparent(c2c.offset(0, -1, 0), chunk)
              }
          }
        }
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

  def updateSunlight(chunk: Chunk, coords: BlockRelChunk): Unit = {
    chunkCache.clearCache()
    chunk.lighting.setSunlight(coords, 0)
    removeSunlight(chunk, coords)
  }

  def removeTorchlight(chunk: Chunk, coords: BlockRelChunk): Unit = {
    chunkCache.clearCache()
    val queue = mutable.Queue[(BlockRelChunk, Chunk, Int)]((coords, chunk, chunk.lighting.getTorchlight(coords)))
    chunk.lighting.setTorchlight(coords, 0)
    propagateTorchlightRemoval(queue)
  }

  def removeSunlight(chunk: Chunk, coords: BlockRelChunk): Unit = {
    chunkCache.clearCache()
    val queue = mutable.Queue[(BlockRelChunk, Chunk, Int)]((coords, chunk, chunk.lighting.getSunlight(coords)))
    chunk.lighting.setSunlight(coords, 0)
    propagateSunlightRemoval(queue)
  }

  private def propagateTorchlightRemoval(queue: mutable.Queue[(BlockRelChunk, Chunk, Int)]): Unit = {
    val lightQueue = mutable.Queue.empty[(BlockRelChunk, Chunk)]

    val chunksNeedingRenderUpdate = mutable.HashSet.empty[Chunk]

    while (queue.nonEmpty) {
      val top = queue.dequeue()
      val here = top._1
      val chunk = top._2
      val level = top._3

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
          } else chunksNeedingRenderUpdate += neigh // the if-case above gets handled later since it's in the queue
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
      val top = queue.dequeue()
      val here = top._1
      val chunk = top._2
      val level = top._3

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
          } else chunksNeedingRenderUpdate += neigh // the if-case above gets handled later since it's in the queue
        }
      }
    }

    chunksNeedingRenderUpdate.foreach(_.requestRenderUpdate())
    propagateSunlight(lightQueue)
  }

  private def propagateTorchlight(queue: mutable.Queue[(BlockRelChunk, Chunk)]): Unit = {
    val chunksNeedingRenderUpdate = mutable.HashSet.empty[Chunk]

    while (queue.nonEmpty) {
      val top = queue.dequeue()
      val here = top._1
      val chunk = top._2

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
            } else chunksNeedingRenderUpdate += neigh // the if-case above gets handled later since it's in the queue
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

  private def propagateSunlightInChunk(chunk: Chunk, queue: mutable.Queue[Int]): Map[Chunk, Seq[Int]] = {
    val neighborMap: mutable.Map[Chunk, mutable.ArrayBuffer[Int]] = mutable.Map.empty
    val inQueue: java.util.BitSet = new java.util.BitSet(16*16*16)

    while (queue.nonEmpty) {
      val hereValue = queue.dequeue()
      val here = BlockRelChunk(hereValue)
      inQueue.clear(hereValue)

      val nextLevel = chunk.lighting.getSunlight(here) - 1

      if (nextLevel > 0) {
        var s = 0
        while (s < 8) { // done as an optimization
          if (here.onChunkEdge(s)) {
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

  private def oppositeSide(s: Int): Int = {
    if (s < 2) 1 - s else (s - 2 + 3) % 3 + 2
  }
}

package hexacraft.world

import hexacraft.math.MathUtils.oppositeSide
import hexacraft.util.Loop
import hexacraft.world.chunk.{Chunk, LocalBlockState}
import hexacraft.world.coord.{BlockRelChunk, BlockRelWorld, ChunkRelWorld}

import scala.collection.mutable

class LightPropagator(world: BlocksInWorld, requestRenderUpdate: ChunkRelWorld => Unit)(using CylinderSize) {
  private val chunkCache: ChunkCache = new ChunkCache(world)

  def initBrightnesses(chunkCoords: ChunkRelWorld): Unit = {
    chunkCache.clearCache()

    val chunk = chunkCache.getChunk(chunkCoords)
    require(chunk != null)

    val queueTorch = mutable.Queue.empty[(BlockRelChunk, ChunkRelWorld, Chunk)]
    val queueSun15 = mutable.Queue.empty[(BlockRelChunk, ChunkRelWorld, Chunk)]
    val queueSunFromNeighbor = mutable.Queue.empty[(BlockRelChunk, ChunkRelWorld, Chunk)]

    Loop.array(chunk.blocks) { case LocalBlockState(c, b) =>
      if b.blockType.lightEmitted != 0 then {
        chunk.setTorchlight(c, b.blockType.lightEmitted)
        queueTorch.enqueue((c, chunkCoords, chunk))
      }
    }

    def shouldEnqueueSunlight(coords: BlockRelChunk, neighCoords: ChunkRelWorld, neigh: Chunk) = {
      val block = neigh.getBlock(coords)
      val neighCol = world.getColumn(neighCoords.getColumnRelWorld).get
      if neighCoords.Y.toInt * 16 + coords.cy > neighCol.terrainHeight.getHeight(coords.cx, coords.cz) then {
        val transparentTop = !block.blockType.isCovering(block.metadata, 0) || block.blockType.isTransmissive
        val transparentBottom = !block.blockType.isCovering(block.metadata, 1) || block.blockType.isTransmissive
        transparentTop && transparentBottom
      } else {
        false
      }
    }

    def handleEdge(x: Int, y: Int, z: Int) = {
      val c2 = BlockRelWorld(x, y, z, chunkCoords)
      val c2c = c2.getBlockRelChunk
      val isAboveChunk = y == 16 && (x & ~15 | z & ~15) == 0

      val neighCoords = c2.getChunkRelWorld
      chunkCache.getChunk(neighCoords) match {
        case null =>
          if isAboveChunk && shouldEnqueueSunlight(c2c.offset(0, -1, 0), chunkCoords, chunk) then {
            chunk.setSunlight(c2c.offset(0, -1, 0), 15)
            queueSun15.enqueue((c2c.offset(0, -1, 0), chunkCoords, chunk))
          }
        case neigh =>
          if neigh.getTorchlight(c2c) > 1 then {
            queueTorch.enqueue((c2c, neighCoords, neigh))
          }

          if isAboveChunk && shouldEnqueueSunlight(c2c, neighCoords, neigh) then {
            queueSun15.enqueue((c2c, neighCoords, neigh))
          }

          val lightHere = neigh.getSunlight(c2c)
          if lightHere > 0 && lightHere < 15 then {
            queueSunFromNeighbor.enqueue((c2c, neighCoords, neigh))
          }
      }
    }

    Loop.rangeTo(-1, 16) { s =>
      Loop.rangeTo(-1, 16) { t =>
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
    propagateSunlight(removeUndergroundSunlight(chunkCoords, chunk))
  }

  /** Sometimes chunks load out of order which can cause sunlight to spawn underground.
    * This function first removes sunlight below the chunk (if the block above is not also full sunlight)
    * and then it propagates the light from this chunk in case there was indirect sunlight from nearby.
    */
  private def removeUndergroundSunlight(
      chunkCoords: ChunkRelWorld,
      chunk: Chunk
  ): mutable.Queue[(BlockRelChunk, ChunkRelWorld, Chunk)] = {
    val queue = mutable.Queue.empty[(BlockRelChunk, ChunkRelWorld, Chunk)]

    val coordsBelow = chunkCoords.offset(0, -1, 0)
    val chunkBelow = chunkCache.getChunk(coordsBelow)

    if chunkBelow != null then {
      Loop.rangeUntil(0, 16) { cz =>
        Loop.rangeUntil(0, 16) { cx =>
          val bcAbove = BlockRelChunk(cx, 0, cz)
          val bcBelow = BlockRelChunk(cx, 15, cz)

          val sunAbove = chunk.getSunlight(bcAbove) == 15
          val sunBelow = chunkBelow.getSunlight(bcBelow) == 15

          if !sunAbove && sunBelow then {
            removeSunlight(coordsBelow, chunkBelow, bcBelow)
            queue.enqueue((bcAbove, chunkCoords, chunk))
          }
        }
      }
    }

    queue
  }

  def addTorchlight(chunkCoords: ChunkRelWorld, chunk: Chunk, coords: BlockRelChunk, value: Byte): Unit = {
    chunkCache.clearCache()
    chunk.setTorchlight(coords, value)
    propagateTorchlight(mutable.Queue((coords, chunkCoords, chunk)))
  }

  def removeTorchlight(chunkCoords: ChunkRelWorld, chunk: Chunk, coords: BlockRelChunk): Unit = {
    chunkCache.clearCache()
    val lightEdge = propagateTorchlightRemoval(chunkCoords, chunk, coords)
    propagateTorchlight(lightEdge)
  }

  def removeSunlight(chunkCoords: ChunkRelWorld, chunk: Chunk, coords: BlockRelChunk): Unit = {
    chunkCache.clearCache()
    val lightEdge = propagateSunlightRemoval(chunkCoords, chunk, coords)
    propagateSunlight(lightEdge)
  }

  private def propagateTorchlightRemoval(
      chunkCoords: ChunkRelWorld,
      chunk: Chunk,
      coords: BlockRelChunk
  ): mutable.Queue[(BlockRelChunk, ChunkRelWorld, Chunk)] = {
    propagateLightRemoval(_.getTorchlight(_), _.setTorchlight(_, _), true)(chunkCoords, chunk, coords)
  }

  private def propagateSunlightRemoval(
      chunkCoords: ChunkRelWorld,
      chunk: Chunk,
      coords: BlockRelChunk
  ): mutable.Queue[(BlockRelChunk, ChunkRelWorld, Chunk)] = {
    propagateLightRemoval(_.getSunlight(_), _.setSunlight(_, _), false)(chunkCoords, chunk, coords)
  }

  private inline def propagateLightRemoval(
      inline getLight: (Chunk, BlockRelChunk) => Byte,
      inline setLight: (Chunk, BlockRelChunk, Byte) => Unit,
      inline attenuateDown: Boolean
  )(
      chunkCoords: ChunkRelWorld,
      chunk: Chunk,
      coords: BlockRelChunk
  ): mutable.Queue[(BlockRelChunk, ChunkRelWorld, Chunk)] = {
    val currentLight = getLight(chunk, coords)
    setLight(chunk, coords, 0)

    val queue = mutable.Queue((coords, chunkCoords, currentLight))
    val lightQueue = mutable.Queue.empty[(BlockRelChunk, ChunkRelWorld, Chunk)]

    val chunksNeedingRenderUpdate = mutable.HashSet.empty[ChunkRelWorld]

    while queue.nonEmpty do {
      val (here, chunkCoords, level) = queue.dequeue()

      chunksNeedingRenderUpdate += chunkCoords

      Loop.rangeUntil(0, 8) { s =>
        val c2w = here.globalNeighbor(s, chunkCoords)
        val c2 = c2w.getBlockRelChunk
        val neighCoords = c2w.getChunkRelWorld
        val neigh = chunkCache.getChunk(neighCoords)
        if neigh != null then {
          val thisLevel = getLight(neigh, c2)
          if thisLevel != 0 then {
            if thisLevel < level || (!attenuateDown && (s == 1 && level == 15)) then {
              queue.enqueue((c2, neighCoords, thisLevel))
              setLight(neigh, c2, 0)
            } else {
              lightQueue.enqueue((c2, neighCoords, neigh))
            }
          } else {
            chunksNeedingRenderUpdate += neighCoords // the if-case above gets handled later since it's in the queue
          }
        }
      }
    }

    chunksNeedingRenderUpdate.foreach(requestRenderUpdate)
    lightQueue
  }

  private def propagateTorchlight(queue: mutable.Queue[(BlockRelChunk, ChunkRelWorld, Chunk)]): Unit = {
    propagateLight(_.getTorchlight(_), _.setTorchlight(_, _), true)(queue)
  }

  private def propagateSunlight(queue: mutable.Queue[(BlockRelChunk, ChunkRelWorld, Chunk)]): Unit = {
    propagateLight(_.getSunlight(_), _.setSunlight(_, _), false)(queue)
  }

  private inline def propagateLight(
      inline getLight: (Chunk, BlockRelChunk) => Byte,
      inline setLight: (Chunk, BlockRelChunk, Byte) => Unit,
      inline attenuateDown: Boolean
  )(queue: mutable.Queue[(BlockRelChunk, ChunkRelWorld, Chunk)]): Unit = {
    val chunksNeedingRenderUpdate = mutable.HashSet.empty[ChunkRelWorld]

    if queue.nonEmpty then {
      val chunksToProcess = mutable.Map.empty[ChunkRelWorld, mutable.Queue[Int]]
      while queue.nonEmpty do {
        val (here, chunkCoords, chunk) = queue.dequeue()
        chunksToProcess.getOrElseUpdate(chunkCoords, mutable.Queue.empty).enqueue(here.value)
        chunksNeedingRenderUpdate += chunkCoords
      }
      while chunksToProcess.nonEmpty do {
        val chunkCoords = chunksToProcess.keys.maxBy(_.Y.toInt)
        val chunk = chunkCache.getChunk(chunkCoords)
        val thisQueue = chunksToProcess.remove(chunkCoords).get
        val map = propagateLightInChunk(getLight, setLight, attenuateDown)(chunkCoords, chunk, thisQueue)

        Loop.iterate(map.keysIterator) { chunkCoords =>
          chunksToProcess.getOrElseUpdate(chunkCoords, mutable.Queue.empty).enqueueAll(map(chunkCoords))
          chunksNeedingRenderUpdate += chunkCoords
        }
      }
    }

    chunksNeedingRenderUpdate.foreach(requestRenderUpdate)
  }

  // pack in multiples of 4 because neighbor coords will always be different mod 4 in this world
  private inline def chunkIndex(c: ChunkRelWorld): Int = {
    ((c.X.toInt & 3) << 4) + ((c.Z.toInt & 3) << 2) + (c.Y.toInt & 3)
  }

  private inline def propagateLightInChunk(
      inline getLight: (Chunk, BlockRelChunk) => Byte,
      inline setLight: (Chunk, BlockRelChunk, Byte) => Unit,
      inline attenuateDown: Boolean
  )(
      chunkCoords: ChunkRelWorld,
      chunk: Chunk,
      queue: mutable.Queue[Int]
  ): collection.Map[ChunkRelWorld, Seq[Int]] = {

    val chunks = new Array[Chunk](4 * 4 * 4)
    val chunkCached = new Array[Boolean](4 * 4 * 4)

    val neighborMap: mutable.Map[ChunkRelWorld, mutable.ArrayBuffer[Int]] = mutable.Map.empty
    val inQueue: java.util.BitSet = new java.util.BitSet(16 * 16 * 16)

    while queue.nonEmpty do {
      val hereValue = queue.dequeue()
      val here = BlockRelChunk(hereValue)
      inQueue.clear(hereValue)

      val nextLevel: Byte = (getLight(chunk, here) - 1).toByte

      if nextLevel > 0 then {
        Loop.rangeUntil(0, 8) { s =>
          if here.isOnChunkEdge(s) then {
            val c2w = here.globalNeighbor(s, chunkCoords)
            val ncc = c2w.getChunkRelWorld
            val c2 = c2w.getBlockRelChunk

            val chunkIdx = chunkIndex(ncc)
            if !chunkCached(chunkIdx) then {
              chunkCached(chunkIdx) = true
              chunks(chunkIdx) = world.getChunk(ncc).orNull
            }
            val neigh = chunks(chunkIdx)
            if neigh != null then {
              val otherSide = oppositeSide(s)

              // The neighboring chunk edge might be in light. This line ensures that it is indeed render-updated.
              if (!neighborMap.contains(ncc)) neighborMap.put(ncc, mutable.ArrayBuffer.empty)

              val bl = neigh.getBlock(c2)
              if !bl.blockType.isCovering(bl.metadata, otherSide) then {
                val thisSLevel = getLight(neigh, c2)
                val nextS: Byte =
                  if !attenuateDown && nextLevel == 14 && s == 1 then (nextLevel + 1).toByte else nextLevel

                if thisSLevel < nextS then {
                  setLight(neigh, c2, nextS)
                  neighborMap(ncc).append(c2.value)
                }
              }
            }
          } else {
            val c2 = here.neighbor(s)
            val otherSide = oppositeSide(s)

            val bl = chunk.getBlock(c2)
            if !bl.blockType.isCovering(bl.metadata, otherSide) then {
              val thisSLevel = getLight(chunk, c2)
              val nextS: Byte =
                if !attenuateDown && nextLevel == 14 && s == 1 then (nextLevel + 1).toByte else nextLevel
              if thisSLevel < nextS then {
                setLight(chunk, c2, nextS)

                if !inQueue.get(c2.value) then {
                  inQueue.set(c2.value)
                  queue += c2.value
                }
              }
            }
          }
        }
      }
    }

    val result = mutable.HashMap.empty[ChunkRelWorld, Seq[Int]]
    Loop.iterate(neighborMap.iterator) { (coords, blocks) =>
      result += coords -> blocks.toSeq
    }
    result
  }
}

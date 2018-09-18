package com.martomate.hexacraft.world.lighting

import com.martomate.hexacraft.world.block.state.BlockState
import com.martomate.hexacraft.world.chunk.IChunk
import com.martomate.hexacraft.world.coord.integer.{BlockRelChunk, BlockRelWorld}
import com.martomate.hexacraft.world.worldlike.BlocksInWorld

import scala.collection.mutable

class LightPropagator(world: BlocksInWorld) {

  def initBrightnesses(chunk: IChunk, lights: mutable.HashMap[BlockRelChunk, BlockState]): Unit = {
    val queueTorch = mutable.Queue.empty[(BlockRelChunk, IChunk)]
    val queueSun15 = mutable.Queue.empty[(BlockRelChunk, IChunk)]
    val queueSunFromNeighbor = mutable.Queue.empty[(BlockRelChunk, IChunk)]
    for ((c, b) <- lights) {
      chunk.lighting.setTorchlight(c, b.blockType.lightEmitted)
      queueTorch += ((c, chunk))
    }

    def enqueueIfTransparent(coords: BlockRelChunk, neigh: IChunk): Unit = {
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
          world.getChunk(c2.getChunkRelWorld) match {
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

  def addTorchlight(chunk: IChunk, coords: BlockRelChunk, value: Int): Unit = {
    chunk.lighting.setTorchlight(coords, value)
    val queue = mutable.Queue(coords -> chunk)
    propagateTorchlight(queue)
  }

  def updateSunlight(chunk: IChunk, coords: BlockRelChunk): Unit = {
    chunk.lighting.setSunlight(coords, 0)
    removeSunlight(chunk, coords)
  }

  def removeTorchlight(chunk: IChunk, coords: BlockRelChunk): Unit = {
    val queue = mutable.Queue[(BlockRelChunk, IChunk, Int)]((coords, chunk, chunk.lighting.getTorchlight(coords)))
    chunk.lighting.setTorchlight(coords, 0)
    propagateTorchlightRemoval(queue)
  }

  def removeSunlight(chunk: IChunk, coords: BlockRelChunk): Unit = {
    val queue = mutable.Queue[(BlockRelChunk, IChunk, Int)]((coords, chunk, chunk.lighting.getSunlight(coords)))
    chunk.lighting.setSunlight(coords, 0)
    propagateSunlightRemoval(queue)
  }

  def propagateTorchlightRemoval(queue: mutable.Queue[(BlockRelChunk, IChunk, Int)]): Unit = {
    val lightQueue = mutable.Queue.empty[(BlockRelChunk, IChunk)]

    val chunksNeedingRenderUpdate = mutable.HashSet.empty[IChunk]

    while (queue.nonEmpty) {
      val top = queue.dequeue()
      val here = top._1
      val chunk = top._2
      val level = top._3

      chunksNeedingRenderUpdate += chunk

      for (s <- BlockState.neighborOffsets.indices) {
        world.neighbor(s, chunk, here) match {
          case (c2, Some(neigh)) =>
            val thisLevel = neigh.lighting.getTorchlight(c2)
            if (thisLevel != 0) {
              if (thisLevel < level) {
                queue += ((c2, neigh, thisLevel))
                neigh.lighting.setTorchlight(c2, 0)
              } else {
                lightQueue += ((c2, neigh))
              }
            } else chunksNeedingRenderUpdate += neigh // the if-case above gets handled later since it's in the queue
          case _ =>
        }
      }
    }

    chunksNeedingRenderUpdate.foreach(_.requestRenderUpdate())

    propagateTorchlight(lightQueue)
  }

  def propagateSunlightRemoval(queue: mutable.Queue[(BlockRelChunk, IChunk, Int)]): Unit = {
    val lightQueue = mutable.Queue.empty[(BlockRelChunk, IChunk)]

    val chunksNeedingRenderUpdate = mutable.HashSet.empty[IChunk]

    while (queue.nonEmpty) {
      val top = queue.dequeue()
      val here = top._1
      val chunk = top._2
      val level = top._3

      chunksNeedingRenderUpdate += chunk

      for (s <- BlockState.neighborOffsets.indices) {
        world.neighbor(s, chunk, here) match {
          case (c2, Some(neigh)) =>
            val thisLevel = neigh.lighting.getSunlight(c2)
            if (thisLevel != 0) {
              if (thisLevel < level || (s == 1 && level == 15)) {
                queue += ((c2, neigh, thisLevel))
                neigh.lighting.setSunlight(c2, 0)
              } else {
                lightQueue += ((c2, neigh))
              }
            } else chunksNeedingRenderUpdate += neigh // the if-case above gets handled later since it's in the queue
          case _ =>
        }
      }
    }

    chunksNeedingRenderUpdate.foreach(_.requestRenderUpdate())
    propagateSunlight(lightQueue)
  }

  def propagateTorchlight(queue: mutable.Queue[(BlockRelChunk, IChunk)]): Unit = {
    val chunksNeedingRenderUpdate = mutable.HashSet.empty[IChunk]

    while (queue.nonEmpty) {
      val top = queue.dequeue()
      val here = top._1
      val chunk = top._2

      val nextLevel = chunk.lighting.getTorchlight(here) - 1

      if (nextLevel > 0) {
        chunksNeedingRenderUpdate += chunk

        for (s <- BlockState.neighborOffsets.indices) {
          world.neighbor(s, chunk, here) match {
            case (c2, Some(neigh)) =>
              val block = neigh.getBlock(c2)
              if (block.blockType.isTransparent(block.metadata, oppositeSide(s))) {
                val thisTLevel = neigh.lighting.getTorchlight(c2)
                if (thisTLevel < nextLevel) {
                  neigh.lighting.setTorchlight(c2, nextLevel)
                  queue += ((c2, neigh))
                }
              } else chunksNeedingRenderUpdate += neigh // the if-case above gets handled later since it's in the queue
            case _ =>
          }
        }
      }
    }

    chunksNeedingRenderUpdate.foreach(_.requestRenderUpdate())
  }

  def propagateSunlight(queue: mutable.Queue[(BlockRelChunk, IChunk)]): Unit = {
    val chunksNeedingRenderUpdate = mutable.HashSet.empty[IChunk]

    while (queue.nonEmpty) {
      val top = queue.dequeue()
      val here = top._1
      val chunk = top._2

      val nextLevel = chunk.lighting.getSunlight(here) - 1

      if (nextLevel > 0) {
        chunksNeedingRenderUpdate += chunk

        for (s <- BlockState.neighborOffsets.indices) {
          world.neighbor(s, chunk, here) match {
            case (c2, Some(neigh)) =>
              val block = neigh.getBlock(c2)
              if (block.blockType.isTransparent(block.metadata, oppositeSide(s))) {
                val thisSLevel = neigh.lighting.getSunlight(c2)
                val nextS = if (nextLevel == 14 && s == 1) nextLevel + 1 else nextLevel
                if (thisSLevel < nextS) {
                  neigh.lighting.setSunlight(c2, nextS)
                  queue += ((c2, neigh))
                } else chunksNeedingRenderUpdate += neigh // the if-case above gets handled later since it's in the queue
              }
            case _ =>
          }
        }
      }
    }

    chunksNeedingRenderUpdate.foreach(_.requestRenderUpdate())
  }

  private def oppositeSide(s: Int) = {
    if (s < 2) 1 - s else (s - 2 + 3) % 3 + 2
  }
}

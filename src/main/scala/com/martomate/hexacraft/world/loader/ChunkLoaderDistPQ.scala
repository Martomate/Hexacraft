package com.martomate.hexacraft.world.loader

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.World
import com.martomate.hexacraft.world.chunk.Chunk
import com.martomate.hexacraft.world.coord.fp.BlockCoords
import com.martomate.hexacraft.world.coord.integer.{BlockRelWorld, ChunkRelWorld}

import scala.collection.mutable
import scala.concurrent.Future

class ChunkLoaderDistPQ(
    origin: PosAndDir,
    chunkFactory: ChunkRelWorld => Chunk,
    chunkUnloader: ChunkRelWorld => Unit,
    maxDist: Double,
    chillSwitch: () => Boolean
)(implicit cylSize: CylinderSize)
    extends ChunkLoader {
  import scala.concurrent.ExecutionContext.Implicits.global

  private val LoadsPerTick = 1
  private val UnloadsPerTick = 2
  private val MaxChunksToLoad = 4
  private val MaxChunksToUnload = 4

  private val prioritizer: ChunkLoadingPrioritizer =
    new ChunkLoadingPrioritizerPQ(origin, distSqFunc, maxDist)

  private def distSqFunc(p: PosAndDir, c: ChunkRelWorld): Double =
    p.pos.distanceSq(BlockCoords(BlockRelWorld(8, 8, 8, c)).toCylCoords)

  private val chunksLoading: mutable.Map[ChunkRelWorld, Future[Chunk]] = mutable.Map.empty
  private val chunksUnloading: mutable.Map[ChunkRelWorld, Future[ChunkRelWorld]] = mutable.Map.empty

  override def tick(): Unit = {
    prioritizer.tick()
    val (maxLoad, maxUnload) = if (chillSwitch()) (1, 1) else (MaxChunksToLoad, MaxChunksToUnload)
    for (_ <- 1 to LoadsPerTick) {
      if (chunksLoading.size < maxLoad) {
        prioritizer.nextAddableChunk.foreach { coords =>
          chunksLoading(coords) = Future(chunkFactory(coords))
          prioritizer += coords
        }
      }
    }
    for (_ <- 1 to UnloadsPerTick) {
      if (chunksUnloading.size < maxUnload) {
        prioritizer.nextRemovableChunk.foreach { coords =>
          chunksUnloading(coords) = Future {
            chunkUnloader(coords)
            coords
          }
          prioritizer -= coords
        }
      }
    }
  }

  override def chunksToAdd(): Iterable[Chunk] =
    chunksLoading.values.flatMap(_.value).flatMap(_.toOption)

  override def chunksToRemove(): Iterable[ChunkRelWorld] =
    chunksUnloading.values.flatMap(_.value).flatMap(_.toOption)

  override def unload(): Unit = prioritizer.unload()

  def onWorldEvent(event: World.Event): Unit =
    event match
      case World.Event.ChunkAdded(chunk)   => chunksLoading -= chunk.coords
      case World.Event.ChunkRemoved(chunk) => chunksUnloading -= chunk.coords
}

package com.martomate.hexacraft.world.loader

import com.martomate.hexacraft.world.chunk.IChunk
import com.martomate.hexacraft.world.column.ChunkColumn
import com.martomate.hexacraft.world.coord.fp.BlockCoords
import com.martomate.hexacraft.world.coord.integer.{BlockRelWorld, ChunkRelWorld}

import scala.collection.mutable
import scala.concurrent.Future

class ChunkLoaderDistPQ(origin: PosAndDir,
                        chunkFactory: ChunkRelWorld => IChunk,
                        chunkUnloader: ChunkRelWorld => Unit,
                        maxDist: Double
                       ) extends ChunkLoader {
  import scala.concurrent.ExecutionContext.Implicits.global

  private val LoadsPerTick = 1
  private val UnloadsPerTick = 4
  private val MaxChunksToLoad = 4
  private val MaxChunksToUnload = 4

  private val prioritizer: ChunkLoadingPrioritizer = new ChunkLoadingPrioritizerPQ(origin, distSqFunc, maxDist)

  private def distSqFunc(p: PosAndDir, c: ChunkRelWorld): Double =
    p.pos.distanceSq(BlockCoords(BlockRelWorld(8, 8, 8, c))(c.cylSize).toCylCoords)

  private val chunksToLoad: mutable.Map[ChunkRelWorld, Future[IChunk]] = mutable.Map.empty
  private val chunksToUnload: mutable.Map[ChunkRelWorld, Future[ChunkRelWorld]] = mutable.Map.empty

  override def tick(): Unit = {
    prioritizer.tick()
    for (_ <- 1 to LoadsPerTick) {
      if (chunksToLoad.size < MaxChunksToLoad) {
        prioritizer.nextAddableChunk.foreach { coords =>
          chunksToLoad(coords) = Future(chunkFactory(coords))
          prioritizer += coords
        }
      }
    }
    for (_ <- 1 to UnloadsPerTick) {
      if (chunksToUnload.size < MaxChunksToUnload) {
        prioritizer.nextRemovableChunk.foreach { coords =>
          chunksToUnload(coords) = Future {
            chunkUnloader(coords)
            coords
          }
          prioritizer -= coords
        }
      }
    }
  }

  override def chunksToAdd(): Iterable[IChunk] =
    chunksToLoad.values.filter(_.isCompleted).flatMap(_.value).flatMap(_.toOption)

  override def chunksToRemove(): Iterable[ChunkRelWorld] =
    chunksToUnload.values.filter(_.isCompleted).flatMap(_.value).flatMap(_.toOption)

  override def unload(): Unit = prioritizer.unload()

  override def onChunkAdded(chunk: IChunk): Unit = {
    chunksToLoad -= chunk.coords
  }
  override def onChunkRemoved(chunk: IChunk): Unit = {
    chunksToUnload -= chunk.coords
  }

  @deprecated
  override def onColumnAdded(column: ChunkColumn): Unit = ()
  @deprecated
  override def onColumnRemoved(column: ChunkColumn): Unit = ()
}

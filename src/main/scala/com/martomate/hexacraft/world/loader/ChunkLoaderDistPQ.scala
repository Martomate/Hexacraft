package com.martomate.hexacraft.world.loader

import com.martomate.hexacraft.world.chunk.IChunk
import com.martomate.hexacraft.world.column.ChunkColumn
import com.martomate.hexacraft.world.coord.fp.BlockCoords
import com.martomate.hexacraft.world.coord.integer.{BlockRelWorld, ChunkRelWorld}

import scala.collection.mutable
import scala.concurrent.Future

class ChunkLoaderDistPQ(origin: PosAndDir, chunkFactory: ChunkRelWorld => IChunk, maxDist: Double) extends ChunkLoader {
  import scala.concurrent.ExecutionContext.Implicits.global

  private val MaxChunksToLoad = 4
  private val MaxChunksToUnload = 4

  private val prioritizer: ChunkLoadingPrioritizer = new ChunkLoadingPrioritizerSimple(origin, distSqFunc, maxDist)

  private def distSqFunc(p: PosAndDir, c: ChunkRelWorld): Double =
    p.pos.distanceSq(BlockCoords(BlockRelWorld(8, 8, 8, c))(c.cylSize).toCylCoords)

  private var chunksToLoad: mutable.Map[ChunkRelWorld, Future[IChunk]] = mutable.Map.empty
  private var chunksToUnload: mutable.Set[ChunkRelWorld] = mutable.Set.empty

  override def tick(): Unit = {
    prioritizer.tick()
    if (chunksToLoad.size < MaxChunksToLoad) {
      prioritizer.nextAddableChunk.foreach(coords => {
        chunksToLoad(coords) = Future(chunkFactory(coords))
        prioritizer += coords
      })
    }
    if (chunksToUnload.size < MaxChunksToUnload) {
      prioritizer.nextRemovableChunk.foreach(coords => {
        chunksToUnload += coords
        prioritizer -= coords
      })
    }
  }//TODO: in the improved version chunk loads and saveIfNeeded can be done in a separate thread

  override def chunksToAdd(): Iterable[IChunk] =
    chunksToLoad.values.filter(_.isCompleted).flatMap(_.value).flatMap(_.toOption)
  override def chunksToRemove(): Iterable[ChunkRelWorld] = chunksToUnload

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

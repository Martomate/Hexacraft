package com.martomate.hexacraft.world.loader

import com.martomate.hexacraft.world.chunk.IChunk
import com.martomate.hexacraft.world.column.ChunkColumn
import com.martomate.hexacraft.world.coord.fp.BlockCoords
import com.martomate.hexacraft.world.coord.integer.{BlockRelWorld, ChunkRelWorld}

class ChunkLoaderDistPQ(origin: PosAndDir, chunkFactory: ChunkRelWorld => IChunk, maxDist: Double) extends ChunkLoader {
  private val prioritizer: ChunkLoadingPrioritizer = new ChunkLoadingPrioritizerSimple(origin, distSqFunc, maxDist)

  private def distSqFunc(p: PosAndDir, c: ChunkRelWorld): Double =
    p.pos.distanceSq(BlockCoords(BlockRelWorld(8, 8, 8, c))(c.cylSize).toCylCoords)

  private var chunkToLoad: IChunk = _
  private var chunkToRemove: ChunkRelWorld = _

  override def tick(): Unit = {
    prioritizer.tick()
    if (chunkToLoad == null)
      prioritizer.nextAddableChunk.foreach(coords => chunkToLoad = chunkFactory(coords))
    if (chunkToRemove == null)
      prioritizer.nextRemovableChunk.foreach(coords => chunkToRemove = coords)
  }

  override def chunksToAdd(): Iterable[IChunk] = Option(chunkToLoad)
  override def chunksToRemove(): Iterable[ChunkRelWorld] = Option(chunkToRemove)

  override def unload(): Unit = prioritizer.unload()

  override def onChunkAdded(chunk: IChunk): Unit = {
    chunkToLoad = null
    prioritizer += chunk.coords
  }
  override def onChunkRemoved(chunk: IChunk): Unit = {
    chunkToRemove = null
    prioritizer -= chunk.coords
  }

  @deprecated
  override def onColumnAdded(column: ChunkColumn): Unit = ()
  @deprecated
  override def onColumnRemoved(column: ChunkColumn): Unit = ()
}

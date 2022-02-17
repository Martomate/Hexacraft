package com.martomate.hexacraft.world.column

import com.martomate.hexacraft.world.chunk.{ChunkBlockListener, ChunkEventListener, IChunk}
import com.martomate.hexacraft.world.collision.CollisionDetector
import com.martomate.hexacraft.world.coord.integer.{ChunkRelColumn, ColumnRelWorld}

object ChunkColumn {
  val neighbors: Seq[(Int, Int)] = Seq(
    (1, 0),
    (0, 1),
    (-1, 0),
    (0, -1))
}

trait ChunkColumn extends ChunkBlockListener with ChunkEventListener {
  def coords: ColumnRelWorld

  private[world] val generatedHeightMap: IndexedSeq[IndexedSeq[Short]]

  def isEmpty: Boolean
  def heightMap(x: Int, z: Int): Short

  def getChunk(coords: ChunkRelColumn): Option[IChunk]
  def setChunk(chunk: IChunk): Unit
  def removeChunk(coords: ChunkRelColumn): Option[IChunk]
  def allChunks: Iterable[IChunk]

  def tick(collisionDetector: CollisionDetector): Unit
  def onReloadedResources(): Unit
  def unload(): Unit

  def addEventListener(listener: ChunkColumnListener): Unit
  def removeEventListener(listener: ChunkColumnListener): Unit
}

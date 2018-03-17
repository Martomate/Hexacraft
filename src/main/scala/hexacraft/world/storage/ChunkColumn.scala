package hexacraft.world.storage

import hexacraft.block.{Block, BlockAir, BlockState}
import hexacraft.world.coord.{BlockCoords, BlockRelColumn, ChunkRelColumn, ColumnRelWorld}
import hexacraft.world.gen.noise.NoiseInterpolator2D
import org.joml.Vector2d

object ChunkColumn {
  val neighbors: Seq[(Int, Int)] = Seq(
    (1, 0),
    (0, 1),
    (-1, 0),
    (0, -1))
}

class ChunkColumn(val coords: ColumnRelWorld, val world: World) {
  val chunks = scala.collection.mutable.Map.empty[Int, Chunk]
  private[storage] var topAndBottomChunks: Option[(Int, Int)] = None
  private[storage] var chunkLoading: Option[Int] = None

  private[storage] val heightMap = {
    val interp = world.worldGenerator.getHeightmapInterpolator(coords)
    
    for (x <- 0 until 16) yield {
      for (z <- 0 until 16) yield {
        interp(x, z).toInt
      }
    }
  }

  def getChunk(coords: ChunkRelColumn): Option[Chunk] = chunks.get(coords.value)

  def getBlock(coords: BlockRelColumn): BlockState = {
    getChunk(coords.getChunkRelColumn).map(_.getBlock(coords.getBlockRelChunk)).getOrElse(BlockAir.State)
  }

  def tick(): Unit = {
    chunks.values.foreach(_.tick())
  }

  private[storage] def updateLoadedChunks(): Unit = {
    val origin = world.chunkLoadingOrigin.toBlockCoord.toVector3d.div(16)
    val xzDist = math.sqrt(coords.distSq(new Vector2d(origin.x, origin.z)))

    def inSight(chunk: ChunkRelColumn): Boolean = {
      val dy = chunk.Y - origin.y
      math.abs(dy) + xzDist * 0.25 <= world.renderDistance
    }
    
    def newTopOrBottom(now: Int, dir: Int): Int = {
      val bottomChunk = ChunkRelColumn(now & 0xfff, world.size)
      if (inSight(bottomChunk)) {
        val below = ChunkRelColumn((now + dir) & 0xfff, world.size)
        if (inSight(below)) world.addChunkToLoadingQueue(below.withColumn(coords))
        now
      } else {
        chunks.remove(bottomChunk.value).foreach(_.unload())
        now - dir
      }
    }

    topAndBottomChunks match {
      case Some((top, bottom)) =>
        val newBottom = newTopOrBottom(bottom, -1)
        val newTop = if (newBottom > top) top else newTopOrBottom(top, 1)
        
        if (newTop != top || newBottom != bottom) topAndBottomChunks = if (newTop >= newBottom) Some((newTop, newBottom)) else None
      case None =>
        val ground = ChunkRelColumn((heightMap(8)(8) >> 4) & 0xfff, world.size)
        val first = if (inSight(ground)) ground else ChunkRelColumn(math.round(origin.y).toInt & 0xfff, world.size)
        
        if (inSight(first)) world.addChunkToLoadingQueue(first.withColumn(coords))
    }
  }

  def unload(): Unit = {
    chunks.values.foreach(_.unload())
  }
}

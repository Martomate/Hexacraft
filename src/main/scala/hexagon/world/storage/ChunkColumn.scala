package hexagon.world.storage

import hexagon.world.coord.ColumnRelWorld
import hexagon.world.coord.ChunkRelColumn
import hexagon.world.coord.BlockRelColumn
import hexagon.Camera
import hexagon.world.coord.CylCoord
import org.joml.Vector2d
import hexagon.world.coord.CoordUtils
import hexagon.world.coord.ChunkRelWorld
import org.joml.Vector3d
import scala.concurrent.Future
import hexagon.world.coord.BlockCoord
import hexagon.world.gen.noise.NoiseInterpolator2D
import hexagon.world.gen.noise.NoiseInterpolator2D
import hexagon.block.BlockState

object ChunkColumn {
  val neighbors = Seq(
    (1, 0),
    (0, 1),
    (-1, 0),
    (0, -1))
}

class ChunkColumn(val coords: ColumnRelWorld, val world: World) {
  /* chunks
   * method for loading/unloading top and bottom chunk
  */

  val chunks = scala.collection.mutable.Map.empty[Int, Chunk]
  private[storage] var topAndBottomChunks: Option[(Int, Int)] = None
  private[storage] var chunkLoading: Option[Int] = None

  private[storage] val heightMap = {
    val interp = new NoiseInterpolator2D(4, 4, (i, j) => {
      val c = BlockCoord(coords.X * 16 + i * 4, 0, coords.Z * 16 + j * 4, world).toCylCoord
      val height = world.biomeHeightGenerator.genNoiseFromCylXZ(c)
      val heightVariation = world.biomeHeightVariationGenerator.genNoiseFromCylXZ(c)
      world.heightMapGenerator.genNoiseFromCylXZ(c) * heightVariation * 100 + height * 100
    })
    
    for (x <- 0 until 16) yield {
      for (z <- 0 until 16) yield {
        interp(x, z).toInt
      }
    }
  }
  
  def getChunk(coords: ChunkRelColumn): Option[Chunk] = chunks.get(coords.value)

  def getBlock(coords: BlockRelColumn): Option[BlockState] = getChunk(coords.getChunkRelColumn).flatMap(_.getBlock(coords.getBlockRelChunk))

  def tick(): Unit = {
    chunks.values.foreach(_.tick)
  }

  private[storage] def updateLoadedChunks(): Unit = {
    val origin = world.chunkLoadingOrigin.toBlockCoord.toVector3d.div(16)
    val xzDist = math.sqrt(coords.distSq(new Vector2d(origin.x, origin.z)))

    def inSight(chunk: ChunkRelColumn): Boolean = {
      val dy = chunk.Y - origin.y
      math.abs(dy) + xzDist * 0.25 <= world.renderDistance
    }
    
    def newTopOrBottom(now: Int, dir: Int): Int = {
      val bottomChunk = ChunkRelColumn(now & 0xfff, world)
      if (inSight(bottomChunk)) {
        val below = ChunkRelColumn((now + dir) & 0xfff, world)
        if (inSight(below)) world.addChunkToLoadingQueue(below.withColumn(coords))
        now
      } else {
        chunks.remove(bottomChunk.value).foreach(_.unload)
        now - dir
      }
    }

    topAndBottomChunks match {
      case Some((top, bottom)) =>
        val newBottom = newTopOrBottom(bottom, -1)
        val newTop = if (newBottom > top) top else newTopOrBottom(top, 1)
        
        if (newTop != top || newBottom != bottom) topAndBottomChunks = if (newTop >= newBottom) Some((newTop, newBottom)) else None
      case None =>
        val ground = ChunkRelColumn((heightMap(8)(8) >> 4) & 0xfff, world)
        val first = if (inSight(ground)) ground else ChunkRelColumn(math.round(origin.y).toInt & 0xfff, world)
        
        if (inSight(first)) world.addChunkToLoadingQueue(first.withColumn(coords))
    }
  }

  def unload(): Unit = {
    chunks.values.foreach(_.unload)
  }
}

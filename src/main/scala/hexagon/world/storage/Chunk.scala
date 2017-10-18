package hexagon.world.storage

import hexagon.world.coord.BlockRelChunk
import hexagon.world.coord.ChunkRelWorld
import hexagon.world.render.ChunkRenderer
import scala.concurrent.Future
import hexagon.world.coord.BlockCoord
import hexagon.world.coord.CylCoord
import hexagon.world.gen.noise.NoiseInterpolator3D
import hexagon.block.Block
import java.io.FileInputStream
import org.jnbt.CompoundTag
import org.jnbt.NBTInputStream
import java.io.File
import org.jnbt.Tag
import org.jnbt.StringTag
import org.jnbt.ByteArrayTag
import org.jnbt.NBTOutputStream
import java.io.FileOutputStream
import hexagon.util.NBTUtil
import hexagon.block.BlockState

object Chunk {
  val neighborOffsets = Seq(
    (0, 1, 0),
    (1, 0, 0),
    (0, 0, 1),
    (-1, 0, 1),
    (0, -1, 0),
    (-1, 0, 0),
    (0, 0, -1),
    (1, 0, -1))
}

class Chunk(val coords: ChunkRelWorld, val world: World) {
  /* block information arrays
   * methods for updating blocks
   */
  
  val neighbors: Array[Option[Chunk]] = Array.tabulate(8)(i => {
    val (dx, dy, dz) = Chunk.neighborOffsets(i)
    val c2 = ChunkRelWorld(coords.X + dx, coords.Y + dy, coords.Z + dz, world)
    world.getChunk(c2) match {
      case Some(chunk) =>
        chunk.neighbors((i + 4) % 8) = Some(this)
        chunk.requestRenderUpdate
        Some(chunk)
      case None => None
    }
  })

  private var storage: ChunkStorage = new DenseChunkStorage(this)
  private var needsToSave = false

  {
    val file = new File(world.saveDir, "chunks/" + coords.value + ".dat")
    if (file.isFile()) {
      val stream = new NBTInputStream(new FileInputStream(file))
      val nbt = stream.readTag().asInstanceOf[CompoundTag]
      stream.close()
      storage.fromNBT(nbt)
    } else {
      val column = world.getColumn(coords.getColumnRelWorld).get
      
      val noiseInterp = new NoiseInterpolator3D(4, 4, 4, (i, j, k) => {
        val c = BlockCoord(coords.X * 16 + i * 4, coords.Y * 16 + j * 4, coords.Z * 16 + k * 4, world).toCylCoord
        world.blockGenerator.genNoiseFromCyl(c) + world.blockDensityGenerator.genNoiseFromCyl(c) * 0.4
      })
  
      for (i <- 0 until 16; j <- 0 until 16; k <- 0 until 16) {
        val noise = noiseInterp(i, j, k)
        val yToGo = coords.Y * 16 + j - column.heightMap(i)(k)
        val limit = if (yToGo < -6) -0.4 else if (yToGo < 0) -0.4 - (6 + yToGo) * 0.025 else 4
        if (noise > limit) storage.setBlock(new BlockState(BlockRelChunk(i, j, k, world).withChunk(coords),
                      if (yToGo < -5) Block.Stone else if (yToGo < -1) Block.Dirt else Block.Grass))
      }
    }
  }

  private var needsRenderingUpdate = true
  private var _renderer: Option[ChunkRenderer] = Some(new ChunkRenderer(this))
  def renderer: Option[ChunkRenderer] = _renderer
  doRenderUpdate()

  def blocks: ChunkStorage = storage

  def getBlock(coords: BlockRelChunk): Option[BlockState] = storage.getBlock(coords)

  def setBlock(block: BlockState): Boolean = {
    val blockCoord = block.coord.getBlockRelChunk
    val before = getBlock(blockCoord)
    storage.setBlock(block)
    if (before == None || before.get != block) {
      onBlockModified(blockCoord)
    }
    true
  }

  def removeBlock(coords: BlockRelChunk): Boolean = {
    storage.removeBlock(coords)
    onBlockModified(coords)
    true
  }

  private def onBlockModified(coords: BlockRelChunk): Unit = {
    requestRenderUpdate()
    val xx = if (coords.cx == 0) -1 else if (coords.cx == 15) 1 else 0
    val yy = if (coords.cy == 0) -1 else if (coords.cy == 15) 1 else 0
    val zz = if (coords.cz == 0) -1 else if (coords.cz == 15) 1 else 0

    for (i <- 0 until 8) {
      val off = Chunk.neighborOffsets(i)
      if (off._1 * xx == 1 || off._2 * yy == 1 || off._3 * zz == 1) neighbors(i).foreach(_.requestRenderUpdate)
    }
    
    needsToSave = true
  }
  
  def requestRenderUpdate(): Unit = {
    if (!needsRenderingUpdate) {
      needsRenderingUpdate = true
      world.addRenderUpdate(coords)
    }
  }
  
  def doRenderUpdate(): Unit = {
    if (needsRenderingUpdate) {
      needsRenderingUpdate = false
      renderer match {
        case Some(r) =>
          if (isEmpty) {
            r.unload()
            _renderer = None
          }
        case None =>
          if (!isEmpty) _renderer = Some(new ChunkRenderer(this))
      }

      renderer.foreach(_.updateContent)
    }
  }

  def tick(): Unit = {
    if (storage.isDense) {
      if (storage.numBlocks < 48) {
        storage = storage.toSparse()
      }
    } else {
      if (storage.numBlocks > 64) {
        storage = storage.toDense()
      }
    }
  }

  def isEmpty(): Boolean = {
    storage.numBlocks == 0
  }

  def unload(): Unit = {
    if (needsToSave) {
      val chunkTag = NBTUtil.makeCompoundTag("chunk", storage.toNBT)// Add more tags with ++
      NBTUtil.saveTag(chunkTag, new File(world.saveDir, "chunks/" + coords.value + ".dat"))
    }
    
    neighbors.foreach(c => c.foreach(_.requestRenderUpdate))
    renderer.foreach(_.unload)
    // and other stuff
  }

  //  def render(): Unit = {
  // TODO: implement system (in Loader) for rendering stuff, like e.g. blocks, with instancing etc. Then: render a block!
  // It might be a good idea to keep a lot i the buffer and then change the buffer in-place when blocks are added/removed
  // It might also be a good idea to have several buffers so that waiting time can be reduced
  // Consider using GL30.glMapBufferRange(target, offset, length, access)
  //  }
}

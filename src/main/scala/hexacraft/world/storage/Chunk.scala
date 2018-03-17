package hexacraft.world.storage

import java.io.File

import hexacraft.block.BlockState
import hexacraft.util.NBTUtil
import hexacraft.world.coord.{BlockRelChunk, BlockRelWorld, ChunkRelWorld}
import hexacraft.world.render.ChunkRenderer

import scala.collection.mutable

object Chunk {
  val neighborOffsets: Seq[(Int, Int, Int)] = Seq(
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
  val neighbors: Array[Option[Chunk]] = Array.tabulate(8){ i =>
    val (dx, dy, dz) = Chunk.neighborOffsets(i)
    val c2 = ChunkRelWorld.offset(coords, dx, dy, dz)
    val chunkOpt = Option(world).flatMap(_.getChunk(c2))
    chunkOpt.foreach(chunk => {
      chunk.neighbors((i + 4) % 8) = Some(this)
      chunk.requestRenderUpdate()
    })
    chunkOpt
  }

  private val generator = new ChunkGenerator(this)
  private val chunkData = generator.loadData()

  private def storage: ChunkStorage = chunkData.storage
  private var needsToSave = false

  private val needsBlockUpdate = mutable.TreeSet.empty[Long]
  private var needsBlockUpdateListener: BlockRelWorld => Unit = world.addToBlockUpdateList

  private var needsRenderingUpdate = true
  private var needsRenderingUpdateListener: ChunkRelWorld => Unit = world.addRenderUpdate
  private var _renderer: Option[ChunkRenderer] = None
  def renderer: Option[ChunkRenderer] = _renderer
  doRenderUpdate()

  def blocks: ChunkStorage = storage

  def getBlock(coords: BlockRelChunk): Option[BlockState] = storage.getBlock(coords)

  def setBlock(block: BlockState): Boolean = {
    val blockCoords = block.coords.getBlockRelChunk
    val before = getBlock(blockCoords)
    storage.setBlock(block)
    if (before.isEmpty || before.get != block) {
      onBlockModified(blockCoords)
    }
    true
  }

  def removeBlock(coords: BlockRelChunk): Boolean = {
    storage.removeBlock(coords)
    onBlockModified(coords)
    true
  }

  private def onBlockModified(coords: BlockRelChunk): Unit = {
    def affectableChunkOffset(where: Byte): Int = if (where == 0) -1 else if (where == 15) 1 else 0

    def isInNeighborChunk(chunkOffset: (Int, Int, Int)) = {
      val xx = affectableChunkOffset(coords.cx)
      val yy = affectableChunkOffset(coords.cy)
      val zz = affectableChunkOffset(coords.cz)

      chunkOffset._1 * xx == 1 || chunkOffset._2 * yy == 1 || chunkOffset._3 * zz == 1
    }

    def offsetCoords(c: BlockRelChunk, off: (Int, Int, Int)) = BlockRelChunk.offset(c, off._1, off._2, off._3)


    requestRenderUpdate()

    for (i <- 0 until 8) {
      val off = Chunk.neighborOffsets(i)
      val c2 = offsetCoords(coords, off)
      if (isInNeighborChunk(off)) {
        neighbors(i).foreach(n => {
          n.requestRenderUpdate()
          n.requestBlockUpdate(c2)
        })
      }
      else requestBlockUpdate(c2)
    }

    requestBlockUpdate(coords)
    needsToSave = true
  }

  def requestBlockUpdate(coords: BlockRelChunk): Unit = {
    if (!needsBlockUpdate(coords.value)) {
      needsBlockUpdate(coords.value) = true
      needsBlockUpdateListener(coords.withChunk(this.coords))
    }
  }

  def doBlockUpdate(coords: BlockRelChunk): Unit = {
    if (needsBlockUpdate(coords.value)) {
      needsBlockUpdate(coords.value) = false
      getBlock(coords).foreach(b => b.blockType.doUpdate(b, world))
    }
  }
  
  def requestRenderUpdate(): Unit = {
    if (!needsRenderingUpdate) {
      needsRenderingUpdate = true
      needsRenderingUpdateListener(coords)
    }
  }
  
  def doRenderUpdate(): Unit = {
    if (needsRenderingUpdate) {
      needsRenderingUpdate = false

      removeRendererIfUnused()

      renderer.foreach(_.updateContent())
    }
  }

  private def removeRendererIfUnused(): Unit = {
    renderer match {
      case Some(r) =>
        if (isEmpty) {
          r.unload()
          _renderer = None
        }
      case None =>
        if (!isEmpty) _renderer = Some(new ChunkRenderer(this))
    }
  }

  def tick(): Unit = {
    chunkData.optimizeStorage()
  }

  def isEmpty: Boolean = storage.numBlocks == 0

  def unload(): Unit = {
    if (needsToSave) {
      val chunkTag = NBTUtil.makeCompoundTag("chunk", storage.toNBT)// Add more tags with ++
      generator.saveData(chunkTag)
    }
    
    neighbors.foreach(c => c.foreach(_.requestRenderUpdate()))
    renderer.foreach(_.unload())
    // and other stuff
  }

  //  def render(): Unit = {
  // TODO: implement system (in Loader) for rendering stuff, like e.g. blocks, with instancing etc. Then: render a block!
  // It might be a good idea to keep a lot i the buffer and then change the buffer in-place when blocks are added/removed
  // It might also be a good idea to have several buffers so that waiting time can be reduced
  // Consider using GL30.glMapBufferRange(target, offset, length, access)
  //  }
}

package com.martomate.hexacraft.world.render

import com.flowpowered.nbt.{CompoundTag, LongTag}
import com.martomate.hexacraft.util.{CylinderSize, NBTUtil}
import com.martomate.hexacraft.world.World
import com.martomate.hexacraft.world.block.Blocks
import com.martomate.hexacraft.world.block.state.BlockState
import com.martomate.hexacraft.world.camera.{Camera, CameraProjection}
import com.martomate.hexacraft.world.chunk._
import com.martomate.hexacraft.world.collision.CollisionDetector
import com.martomate.hexacraft.world.column.{ChunkColumn, ChunkColumnListener}
import com.martomate.hexacraft.world.coord.integer._
import com.martomate.hexacraft.world.gen.WorldGenerator
import com.martomate.hexacraft.world.lighting.{ChunkLighting, LightPropagator}
import com.martomate.hexacraft.world.settings.{WorldGenSettings, WorldSettings, WorldSettingsProvider}
import com.martomate.hexacraft.world.storage.{ChunkStorage, DenseChunkStorage}
import com.martomate.hexacraft.world.worldlike.{BlocksInWorld, IWorld}
import org.scalameter.api._

object ChunkRenderImplBench extends Bench.OnlineRegressionReport {
  implicit val cylSize: CylinderSize = new CylinderSize(8)

  // Initializations ------
  val heightAboveGround: Gen[Int] = Gen.range("Height above ground")(-16, 16, 8)

  /*
  val renderers: Gen[ChunkRendererImpl] = for {
    blocksPerChunk <- blockCounts
    world = new TestWorld(ChunkRelWorld(3, 4, 5).extendedNeighbors(1),
                          (c, w) => new TestChunk(c, w).fillWithBlocks(blocksPerChunk))
    chunk = world.getChunk(ChunkRelWorld(3, 4, 5)).get
  } yield new ChunkRendererImpl(chunk, world)
  */

  var renderers: Gen[ChunkRendererImpl] = setupRenderers

  // Measurements ------
  performance of "ChunkRenderer" in {
    measure method "updateContent" config (
      exec.benchRuns -> 10,
      exec.independentSamples -> 10,
      reports.regression.significance -> 5e-2
    ) in {
      using(renderers) in { renderer =>
        renderer.updateContent()
      }
    }
  }

  // Helper stuff ------
  def setupRenderers: Gen[ChunkRendererImpl] = for {
    heightAboveGround <- heightAboveGround
  } yield {
    val world = initWorld(heightAboveGround)
    val chunk = world.getChunk(ChunkRelWorld(0, (world.getHeight(0, 0) + heightAboveGround * 2) >> 4, 0)).get
    val r = new ChunkRendererImpl(chunk, world)
    r.updateContent() // to initialize lighting
    r
  }

  def initWorld(heightAboveGround: Int): IWorld = {
    val w = new World(new TestWorldSettingsProvider(12398734587123L))
    val cam = new Camera(new CameraProjection(1,1,1,1))
    cam.position.y = w.getHeight(0, 0) + heightAboveGround
    cam.updateViewMatrix()
    for (_ <- 1 to 20) {
      w.tick(cam)
      Thread.sleep(10)
    }
    w
  }

  private class TestWorldSettingsProvider(seed: Long) extends WorldSettingsProvider {
    override def name: String = "bench world"

    override def size: CylinderSize = cylSize

    override def gen: WorldGenSettings = new WorldGenSettings(
      NBTUtil.makeCompoundTag("", Seq(new LongTag("seed", seed))),
      WorldSettings(Some(name), Some(8), Some(seed))
    )

    override def plannerNBT: CompoundTag = NBTUtil.makeCompoundTag("", Seq.empty)

    override def playerNBT: CompoundTag = NBTUtil.makeCompoundTag("", Seq.empty)

    override def loadState(path: String): CompoundTag = NBTUtil.makeCompoundTag("", Seq.empty)

    override def saveState(tag: CompoundTag, path: String): Unit = ()
  }

  private class TestWorld(chunksToBeLoaded: Seq[ChunkRelWorld], chunkFactory: (ChunkRelWorld, IWorld) => IChunk)(implicit val cylSize: CylinderSize) extends IWorld {
    private val loadedColumns = chunksToBeLoaded.map(_.getColumnRelWorld).distinct
      .map(col => col -> new TestColumn(col)).toMap
    private val loadedChunks = chunksToBeLoaded.map(c => c -> chunkFactory(c, this)).toMap

    override val size: CylinderSize = cylSize

    override def worldSettings: WorldSettingsProvider = ???

    override def worldGenerator: WorldGenerator = ???

    override def renderDistance: Double = ???

    override def collisionDetector: CollisionDetector = ???

    override def getHeight(x: Int, z: Int): Int = ???

    override def addChunkAddedOrRemovedListener(listener: ChunkAddedOrRemovedListener): Unit = ???

    override def removeChunkAddedOrRemovedListener(listener: ChunkAddedOrRemovedListener): Unit = ???

    override def setBlock(coords: BlockRelWorld, block: BlockState): Unit = ???

    override def removeBlock(coords: BlockRelWorld): Unit = ???

    override def onSetBlock(coords: BlockRelWorld, prev: BlockState, now: BlockState): Unit = ???

    override def onBlockNeedsUpdate(coords: BlockRelWorld): Unit = ???

    override def onChunkNeedsRenderUpdate(coords: ChunkRelWorld): Unit = ???

    override def onChunksNeighborNeedsRenderUpdate(coords: ChunkRelWorld, side: Int): Unit = ???

    override def getColumn(coords: ColumnRelWorld): Option[ChunkColumn] = loadedColumns.get(coords)

    override def getChunk(coords: ChunkRelWorld): Option[IChunk] = loadedChunks.get(coords)

    override def getBlock(coords: BlockRelWorld): BlockState = ???

    override def provideColumn(coords: ColumnRelWorld): ChunkColumn = ???

    override def onChunkAdded(chunk: IChunk): Unit = ???

    override def onChunkRemoved(chunk: IChunk): Unit = ???
  }

  private class TestColumn(_coords: ColumnRelWorld) extends ChunkColumn {
    override def coords: ColumnRelWorld = _coords

    override private[world] val generatedHeightMap = null

    override def isEmpty: Boolean = ???

    override def heightMap(x: Int, z: Int): Short = ???

    override def getChunk(coords: ChunkRelColumn): Option[IChunk] = ???

    override def setChunk(chunk: IChunk): Unit = ???

    override def removeChunk(coords: ChunkRelColumn): Option[IChunk] = ???

    override def allChunks: Iterable[IChunk] = ???

    override def tick(): Unit = ???

    override def onReloadedResources(): Unit = ???

    override def unload(): Unit = ???

    override def addEventListener(listener: ChunkColumnListener): Unit = ???

    override def removeEventListener(listener: ChunkColumnListener): Unit = ???

    override def onBlockNeedsUpdate(coords: BlockRelWorld): Unit = ???

    override def onChunkNeedsRenderUpdate(coords: ChunkRelWorld): Unit = ???

    override def onChunksNeighborNeedsRenderUpdate(coords: ChunkRelWorld, side: Int): Unit = ???

    override def onSetBlock(coords: BlockRelWorld, prev: BlockState, now: BlockState): Unit = ???
  }

  private class TestChunk(_coords: ChunkRelWorld, _world: BlocksInWorld)(implicit val cylSize: CylinderSize) extends IChunk {
    def fillWithBlocks(count: Int): TestChunk = {
      val storage = blocks
      for (i <- (0 until count).map(_ * 19 % (16*16*16))) {
        storage.setBlock(BlockRelChunk(i), new BlockState(Blocks.Dirt, 0))
      }
      this
    }

    override def isDecorated: Boolean = ???

    override def setDecorated(): Unit = ???

    override val coords: ChunkRelWorld = _coords

    override val lighting: IChunkLighting =
      new ChunkLighting(this, new LightPropagator(_world))

    override def init(): Unit = ???

    override def tick(): Unit = ???

    override def hasNoBlocks: Boolean = false

    override val blocks: ChunkStorage = new DenseChunkStorage(coords)

    override def entities: EntitiesInChunk = ???

    override def addEventListener(listener: ChunkEventListener): Unit = ???

    override def removeEventListener(listener: ChunkEventListener): Unit = ???

    override def addBlockEventListener(listener: ChunkBlockListener): Unit = ???

    override def removeBlockEventListener(listener: ChunkBlockListener): Unit = ???

    override def requestRenderUpdate(): Unit = ???

    override def requestBlockUpdate(coords: BlockRelChunk): Unit = ???

    override def saveIfNeeded(): Unit = ???

    override def unload(): Unit = ???

    override def getBlock(coords: BlockRelChunk): BlockState = blocks.getBlock(coords)

    override def setBlock(blockCoords: BlockRelChunk, block: BlockState): Unit = ???

    override def removeBlock(coords: BlockRelChunk): Unit = ???
  }
}

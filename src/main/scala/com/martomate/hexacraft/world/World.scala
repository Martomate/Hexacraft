package com.martomate.hexacraft.world

import com.flowpowered.nbt.{ByteTag, CompoundTag, ShortTag, StringTag}
import com.martomate.hexacraft.util._
import com.martomate.hexacraft.world.block.state.BlockState
import com.martomate.hexacraft.world.camera.Camera
import com.martomate.hexacraft.world.chunk.{ChunkAddedOrRemovedListener, IChunk}
import com.martomate.hexacraft.world.chunkgen.ChunkGenerator
import com.martomate.hexacraft.world.collision.CollisionDetector
import com.martomate.hexacraft.world.column.{ChunkColumn, ChunkColumnImpl}
import com.martomate.hexacraft.world.coord.CoordUtils
import com.martomate.hexacraft.world.coord.fp.CylCoords
import com.martomate.hexacraft.world.coord.integer.{BlockRelChunk, BlockRelWorld, ChunkRelWorld, ColumnRelWorld, Offset}
import com.martomate.hexacraft.world.entity.loader.EntityModelLoader
import com.martomate.hexacraft.world.entity.Entity
import com.martomate.hexacraft.world.entity.registry.EntityRegistrator
import com.martomate.hexacraft.world.gen.{WorldGenerator, WorldPlanner}
import com.martomate.hexacraft.world.lighting.LightPropagator
import com.martomate.hexacraft.world.loader.{ChunkLoader, ChunkLoaderDistPQ, PosAndDir}
import com.martomate.hexacraft.world.player.Player
import com.martomate.hexacraft.world.save.WorldSave
import com.martomate.hexacraft.world.settings.WorldSettingsProvider
import com.martomate.hexacraft.world.worldlike.IWorld

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object World {
  val ticksBetweenBlockUpdates = 5
  val ticksBetweenEntityRelocation = 120
}

class World(val worldSettings: WorldSettingsProvider) extends IWorld {
  def worldName: String = worldSettings.name
  val size: CylinderSize = worldSettings.size
  import size.impl

  private implicit val modelLoaderImpl: EntityModelLoader = new EntityModelLoader()
  EntityRegistrator.load()

  val worldGenerator = new WorldGenerator(worldSettings.gen)
  private val worldPlanner: WorldPlanner = WorldPlanner(this, worldSettings.plannerNBT)
  private val lightPropagator: LightPropagator = new LightPropagator(this)

  val renderDistance: Double = 8 * CylinderSize.y60

  override val collisionDetector: CollisionDetector = new CollisionDetector(this)

  private val columns = mutable.LongMap.empty[ChunkColumn]

  private val chunkLoadingOrigin = new PosAndDir
  private val chunkLoader: ChunkLoader = new ChunkLoaderDistPQ(
    chunkLoadingOrigin,
    coords => new Chunk(coords, new ChunkGenerator(coords, this), lightPropagator),
    coords => getChunk(coords).foreach(_.saveIfNeeded()),
    renderDistance
  )

  private val blocksToUpdate: UniquePQ[BlockRelWorld] = new UniquePQ(_ => 0, Ordering.by(x => x))

  val player: Player = makePlayer

  private val chunkAddedOrRemovedListeners: ArrayBuffer[ChunkAddedOrRemovedListener] = ArrayBuffer.empty
  def addChunkAddedOrRemovedListener(listener: ChunkAddedOrRemovedListener): Unit = chunkAddedOrRemovedListeners += listener
  def removeChunkAddedOrRemovedListener(listener: ChunkAddedOrRemovedListener): Unit = chunkAddedOrRemovedListeners -= listener

  addChunkAddedOrRemovedListener(worldPlanner)
  addChunkAddedOrRemovedListener(chunkLoader)

  saveWorldData()

  def onBlockNeedsUpdate(coords: BlockRelWorld): Unit = blocksToUpdate.enqueue(coords)

  def onChunkNeedsRenderUpdate(coords: ChunkRelWorld): Unit = ()

  def getColumn(coords: ColumnRelWorld): Option[ChunkColumn] = columns.get(coords.value)

  def getChunk(coords: ChunkRelWorld): Option[IChunk] =
    getColumn(coords.getColumnRelWorld).flatMap(_.getChunk(coords.getChunkRelColumn))

  def getBlock(coords: BlockRelWorld): BlockState =
    getChunk(coords.getChunkRelWorld).map(_.getBlock(coords.getBlockRelChunk)).getOrElse(BlockState.Air)

  def provideColumn(coords: ColumnRelWorld): ChunkColumn = {
    ensureColumnExists(coords)
    getColumn(coords).get
  }

  def setBlock(coords: BlockRelWorld, block: BlockState): Unit =
    getChunk(coords.getChunkRelWorld).foreach(_.setBlock(coords.getBlockRelChunk, block))

  def removeBlock(coords: BlockRelWorld): Unit =
    getChunk(coords.getChunkRelWorld).foreach(_.removeBlock(coords.getBlockRelChunk))

  def addEntity(entity: Entity): Unit = {
    chunkOfEntity(entity).foreach(_.entities += entity)
  }

  def removeEntity(entity: Entity): Unit = {
    chunkOfEntity(entity).foreach(_.entities -= entity)
  }

  def removeAllEntities(): Unit = {
    for (col <- columns.values) {
      for (ch <- col.allChunks) {
        for (e <- ch.entities.allEntities.toSeq) {
          ch.entities -= e
        }
      }
    }
  }

  private def chunkOfEntity(entity: Entity): Option[IChunk] = {
    getApproximateChunk(entity.position)
  }

  private def getApproximateChunk(coords: CylCoords): Option[IChunk] = {
    getChunk(CoordUtils.approximateChunkCoords(coords))
  }

  def getHeight(x: Int, z: Int): Int = {
    val coords = ColumnRelWorld(x >> 4, z >> 4)
    ensureColumnExists(coords)
    getColumn(coords).get.heightMap(x & 15, z & 15)
  }

  def tick(camera: Camera): Unit = {
    chunkLoadingOrigin.setPosAndDirFrom(camera.view)
    chunkLoader.tick()

    addNewChunks()

    removeOldChunks()

    if (blockUpdateTimer.tick()) {
      performBlockUpdates()
    }
    if (relocateEntitiesTimer.tick()) {
      performEntityRelocation()
    }

    columns.values.foreach(_.tick())
  }

  private def removeOldChunks(): Unit = {
    for (ch <- chunkLoader.chunksToRemove()) {
      getColumn(ch.getColumnRelWorld) foreach { col =>
        col.removeChunk(ch.getChunkRelColumn).foreach { c =>
          chunkAddedOrRemovedListeners.foreach(_.onChunkRemoved(c))
          c.unload()
        }
        if (col.isEmpty) columns.remove(col.coords.value).foreach { c =>
          c.removeEventListener(this)
          c.unload()
        }
      }
    }
  }

  private def addNewChunks(): Unit = {
    for (ch <- chunkLoader.chunksToAdd()) {
      ensureColumnExists(ch.coords.getColumnRelWorld)
      getColumn(ch.coords.getColumnRelWorld).get.setChunk(ch)
      chunkAddedOrRemovedListeners.foreach(_.onChunkAdded(ch))
      ch.init()
      worldPlanner.decorate(ch)
    }
  }

  private val blockUpdateTimer: TickableTimer = TickableTimer(World.ticksBetweenBlockUpdates)

  private def performBlockUpdates(): Unit = {
    val blocksToUpdateLen = blocksToUpdate.size
    for (_ <- 0 until blocksToUpdateLen) {
      val c = blocksToUpdate.dequeue()
      getBlock(c).blockType.doUpdate(c, this)
    }
  }

  private val relocateEntitiesTimer: TickableTimer = TickableTimer(World.ticksBetweenEntityRelocation)

  private def performEntityRelocation(): Unit = {
    val entList = for {
      col <- columns.values
      ch <- col.allChunks
      ent <- ch.entities.allEntities
    } yield (ch, ent, chunkOfEntity(ent))

    for ((ch, ent, newOpt) <- entList) newOpt match {
      case Some(newChunk) =>
        if (newChunk != ch) {
          ch.entities -= ent
          newChunk.entities += ent
        }
      case None =>
    }
  }

  private def ensureColumnExists(here: ColumnRelWorld): Unit = {
    if (!columns.contains(here.value)) {
      val col = new ChunkColumnImpl(here, worldGenerator, worldSettings)
      columns(here.value) = col
      col.addEventListener(this)
    }
  }

  def getBrightness(block: BlockRelWorld): Float = {
    if (block != null) getChunk(block.getChunkRelWorld).map(_.lighting.getBrightness(block.getBlockRelChunk)).getOrElse(1.0f)
    else 1.0f
  }

  def onReloadedResources(): Unit = {
    columns.values.foreach(_.onReloadedResources())
  }

  def unload(): Unit = {
    saveWorldData()

    chunkLoader.unload()
    blockUpdateTimer.active = false
    columns.values.foreach(_.unload())
    columns.clear
    chunkAddedOrRemovedListeners.clear()
  }

  private def saveWorldData(): Unit = {
    val worldTag = toNBT

    worldSettings.saveState(worldTag, "world.dat")
  }

  private def toNBT: CompoundTag = {
    NBTUtil.makeCompoundTag("world", Seq(
      new ShortTag("version", WorldSave.LatestVersion),
      NBTUtil.makeCompoundTag("general", Seq(
        new ByteTag("worldSize", size.worldSize.toByte),
        new StringTag("name", worldName)
      )),
      worldGenerator.toNBT,
      worldPlanner.toNBT,
      player.toNBT
    ))
  }

  override def onChunksNeighborNeedsRenderUpdate(coords: ChunkRelWorld, side: Int): Unit = neighborChunk(coords, side).foreach(_.requestRenderUpdate())

  override def onChunkAdded(chunk: IChunk): Unit = chunkAddedOrRemovedListeners.foreach(_.onChunkAdded(chunk))

  override def onChunkRemoved(chunk: IChunk): Unit = chunkAddedOrRemovedListeners.foreach(_.onChunkRemoved(chunk))

  override def onSetBlock(coords: BlockRelWorld, prev: BlockState, now: BlockState): Unit = {
    def affectableChunkOffset(where: Byte): Int = where match {
      case  0 => -1
      case 15 =>  1
      case  _ =>  0
    }

    def isInNeighborChunk(chunkOffset: Offset) = {
      val xx = affectableChunkOffset(coords.cx)
      val yy = affectableChunkOffset(coords.cy)
      val zz = affectableChunkOffset(coords.cz)

      chunkOffset.dx * xx == 1 || chunkOffset.dy * yy == 1 || chunkOffset.dz * zz == 1
    }

    def offsetCoords(c: BlockRelChunk, off: Offset) = c.offset(off)

    val cCoords = coords.getChunkRelWorld
    val bCoords = coords.getBlockRelChunk

    getChunk(cCoords).foreach { c =>
      c.requestRenderUpdate()
      c.requestBlockUpdate(bCoords)

      for (i <- 0 until 8) {
        val off = ChunkRelWorld.neighborOffsets(i)
        val c2 = offsetCoords(bCoords, off)

        if (isInNeighborChunk(off)) {
          neighborChunk(cCoords, i).foreach(n => {
            n.requestRenderUpdate()
            n.requestBlockUpdate(c2)
          })
        }
        else c.requestBlockUpdate(c2)
      }
    }
  }

  private def makePlayer: Player = {
    val player = new Player(this)
    player.fromNBT(worldSettings.playerNBT)
    player
  }
}

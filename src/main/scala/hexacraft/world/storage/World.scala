package hexacraft.world.storage

import java.io.{File, FileInputStream}
import java.util.Random

import com.flowpowered.nbt._
import com.flowpowered.nbt.stream.NBTInputStream
import hexacraft.Camera
import hexacraft.block.{BlockAir, BlockState}
import hexacraft.util.NBTUtil
import hexacraft.world.coord._
import hexacraft.world.{Player, WorldSettings}
import org.joml.{Vector2d, Vector3d, Vector4d}

import scala.collection.mutable
import scala.collection.mutable.{Set => MutableSet}

object World {
  val chunksLoadedPerTick = 2
  val chunkRenderUpdatesPerTick = 2
  val ticksBetweenBlockUpdates = 5
  val ticksBetweenColumnLoading = 5
}

trait ReadableWorld {
  def getColumn(coords: ColumnRelWorld): Option[ChunkColumn]
  def getChunk(coords: ChunkRelWorld): Option[Chunk]
  def getBlock(coords: BlockRelWorld): Option[BlockState]
}

class WorldGenSettings(val nbt: CompoundTag, val defaultSettings: WorldSettings) {
  val seed                        : Long   = NBTUtil.getLong  (nbt, "seed", defaultSettings.seed.getOrElse(new Random().nextLong))
  val blockGenScale               : Double = NBTUtil.getDouble(nbt, "blockGenScale", 0.1)
  val heightMapGenScale           : Double = NBTUtil.getDouble(nbt, "heightMapGenScale", 0.02)
  val blockDensityGenScale        : Double = NBTUtil.getDouble(nbt, "blockDensityGenScale", 0.01)
  val biomeHeightMapGenScale      : Double = NBTUtil.getDouble(nbt, "biomeHeightMapGenScale", 0.002)
  val biomeHeightVariationGenScale: Double = NBTUtil.getDouble(nbt, "biomeHeightVariationGenScale", 0.002)

  def toNBT: CompoundTag = NBTUtil.makeCompoundTag("gen", Seq(
    new LongTag("seed", seed),
    new DoubleTag("blockGenScale", blockGenScale),
    new DoubleTag("heightMapGenScale", heightMapGenScale),
    new DoubleTag("blockDensityGenScale", blockDensityGenScale),
    new DoubleTag("biomeHeightGenScale", biomeHeightMapGenScale),
    new DoubleTag("biomeHeightVariationGenScale", biomeHeightVariationGenScale)
  ))
}

trait WorldSettingsProvider {
  def name: String
  def size: CylinderSize
  def gen: WorldGenSettings
  def playerNBT: CompoundTag

  def loadState(path: String): CompoundTag
  def saveState(tag: CompoundTag, path: String = "world.dat"): Unit
}

class WorldSettingsProviderFromFile(saveDir: File, worldSettings: WorldSettings) extends WorldSettingsProvider {
  private val nbtData: CompoundTag = loadState("world.dat")
  private val generalSettings: CompoundTag = nbtData.getValue.get("general").asInstanceOf[CompoundTag]

  val name: String = NBTUtil.getString(generalSettings, "worldName", worldSettings.name.getOrElse(saveDir.getName))
  val size: CylinderSize = new CylinderSize(NBTUtil.getByte(generalSettings, "worldSize", worldSettings.size.getOrElse(7)))
  def gen: WorldGenSettings = new WorldGenSettings(nbtData.getValue.get("gen").asInstanceOf[CompoundTag], worldSettings)
  def playerNBT: CompoundTag = nbtData.getValue.get("player").asInstanceOf[CompoundTag]

  def loadState(path: String): CompoundTag = {
    val file = new File(saveDir, path)
    if (file.isFile) {
      val stream = new NBTInputStream(new FileInputStream(file))
      val nbt = stream.readTag().asInstanceOf[CompoundTag]
      stream.close()
      nbt
    } else {
      new CompoundTag("", new CompoundMap())
    }
  }

  def saveState(tag: CompoundTag, path: String = "world.dat"): Unit = {
    NBTUtil.saveTag(tag, new File(saveDir, path))
  }
}

class World(val worldSettings: WorldSettingsProvider) {
  /* blockStorage
   * other world contents
   * methods for world interaction
   * method for loading/unloading blockColumns
   * block methods that will be forwarded to the correct chunk
   * worldGen references and methods
   * might rename this class to WorldStorage, and make another class World
   */

  def worldName: String = worldSettings.name
  val size: CylinderSize = worldSettings.size

  val worldGenerator = new WorldGenerator(worldSettings.gen, size)

  val renderDistance: Double = 8 * CoordUtils.y60

  val columns = scala.collection.mutable.Map.empty[Long, ChunkColumn]
  val columnsAtEdge: MutableSet[ColumnRelWorld] = MutableSet.empty[ColumnRelWorld]

  private var loadColumnsCountdown = 0

  private[storage] var chunkLoadingOrigin: CylCoords = _
  private[storage] val chunkLoadingDirection: Vector3d = new Vector3d()
  private val chunksToLoad = mutable.PriorityQueue.empty[(Double, ChunkRelWorld)](Ordering.by(-_._1))

  private val chunkRenderUpdateQueue = mutable.PriorityQueue.empty[(Double, ChunkRelWorld)](Ordering.by(-_._1))

  private val blocksToUpdate = mutable.Queue.empty[BlockRelWorld]

  val player = new Player(this)
  player.fromNBT(worldSettings.playerNBT)


  def addToBlockUpdateList(coords: BlockRelWorld): Unit = blocksToUpdate.enqueue(coords)

  def addRenderUpdate(coords: ChunkRelWorld): Unit = {
    chunkRenderUpdateQueue.enqueue(makeChunkToLoadTuple(coords))
  }

  def addChunkToLoadingQueue(coords: ChunkRelWorld): Unit = {
    chunksToLoad.enqueue(makeChunkToLoadTuple(coords))
  }

  private def makeChunkToLoadTuple(coords: ChunkRelWorld) = {
    val cyl = BlockCoords(coords.withBlockCoords(8, 8, 8), size).toCylCoord
    val cDir = cyl.toNormalCoord(chunkLoadingOrigin).toVector3d.normalize()
    val dot = chunkLoadingDirection.dot(cDir)

    (chunkLoadingOrigin.distanceSq(cyl) * (1.25 - math.pow((dot + 1) / 2, 4)) / 1.25, coords)
  }

  def getColumn(coords: ColumnRelWorld): Option[ChunkColumn] = columns.get(coords.value)
  def getChunk(coords: ChunkRelWorld): Option[Chunk]      = getColumn(coords.getColumnRelWorld).flatMap(_.getChunk(coords.getChunkRelColumn))
  def getBlock(coords: BlockRelWorld): BlockState = getColumn(coords.getColumnRelWorld).map(_.getBlock(coords.getBlockRelColumn)).getOrElse(BlockAir.State)
  def setBlock(coords: BlockRelWorld, block: BlockState): Boolean = getChunk(coords.getChunkRelWorld).fold(false)(_.setBlock(coords.getBlockRelChunk, block))
  def removeBlock(coords: BlockRelWorld): Boolean = getChunk(coords.getChunkRelWorld).fold(false)(_.removeBlock(coords.getBlockRelChunk))
  def requestBlockUpdate(coords: BlockRelWorld): Unit = getChunk(coords.getChunkRelWorld).foreach(_.requestBlockUpdate(coords.getBlockRelChunk))

  def getHeight(x: Int, z: Int): Int = {
    val coords = ColumnRelWorld(x >> 4, z >> 4, size)
    ensureColumnExists(coords)
    getColumn(coords).get.heightMap(x & 15)(z & 15)
  }

  def tick(camera: Camera): Unit = {
    setChunkLoadingCenterAndDirection(camera)

    loadChunkColumns()

    loadChunks()

    performChunkRenderUpdates()

    columns.values.foreach(_.tick())
  }

  private def setChunkLoadingCenterAndDirection(camera: Camera) = {
    chunkLoadingOrigin = CylCoords(player.position.x, player.position.y, player.position.z, size)
    val vec4 = new Vector4d(0, 0, -1, 0).mul(camera.view.invMatrix)
    chunkLoadingDirection.set(vec4.x, vec4.y, vec4.z) // new Vector3d(0, 0, -1).rotateX(-player.rotation.x).rotateY(-player.rotation.y))
  }

  private def performChunkRenderUpdates(): Unit = {
    for (_ <- 1 to World.chunkRenderUpdatesPerTick) {
      if (chunkRenderUpdateQueue.nonEmpty) {
        val chunk = chunkRenderUpdateQueue.dequeue()._2
        getChunk(chunk).foreach(_.doRenderUpdate())
      }
    }
  }

  private def loadChunks(): Unit = {
    for (_ <- 1 to World.chunksLoadedPerTick) {
      if (chunksToLoad.nonEmpty) {
        val chunkToLoad = chunksToLoad.dequeue()._2
        getColumn(chunkToLoad.getColumnRelWorld).foreach { col =>
          val ch = chunkToLoad.getChunkRelColumn
          val chY = ch.Y
          val topBottomChange = col.topAndBottomChunks match {
            case None => Some((chY, chY))
            case Some((top, bottom)) =>
              if (chY == top + 1) Some((chY, bottom))
              else if (chY == bottom - 1) Some((top, chY))
              else None
          }
          if (topBottomChange.isDefined) {
            col.topAndBottomChunks = topBottomChange
            col.chunks(ch.value) = new Chunk(chunkToLoad, this)
          }
        }
      }
    }
  }

  private def loadChunkColumns(): Unit = {
    if (loadColumnsCountdown == 0) {
      loadColumnsCountdown = World.ticksBetweenColumnLoading

      // TODO: this is a temporary placement
      performBlockUpdates()

      filterChunkRenderUpdateQueue()

      updateLoadedColumns()
      columns.values.foreach(_.updateLoadedChunks())
    }
    loadColumnsCountdown -= 1
  }

  private def filterChunkRenderUpdateQueue(): Unit = {
    val rDistSq = (renderDistance * 16) * (renderDistance * 16)

    //      chunksToLoad.enqueue(chunksToLoad.dequeueAll.map(c => makeChunkToLoadTuple(c._2)).filter(c => c._1 <= rDistSq): _*)
    chunksToLoad.clear()
    chunkRenderUpdateQueue.enqueue(chunkRenderUpdateQueue.dequeueAll.map(c => makeChunkToLoadTuple(c._2)).filter(c => c._1 <= rDistSq): _*)
  }

  private def performBlockUpdates(): Unit = {
    val blocksToUpdateLen = blocksToUpdate.size
    for (_ <- 0 until blocksToUpdateLen) {
      val c = blocksToUpdate.dequeue()
      getChunk(c.getChunkRelWorld).foreach(_.doBlockUpdate(c.getBlockRelChunk))
    }
  }

  private def updateLoadedColumns(): Unit = {
    val rDistSq = math.pow(renderDistance, 2)
    val origin = {
      val temp = chunkLoadingOrigin.toBlockCoord
      new Vector2d(temp.x / 16, temp.z / 16)
    }
    def inSight(col: ColumnRelWorld): Boolean = {
      col.distSq(origin) <= rDistSq
    }

    val here = ColumnRelWorld(math.floor(origin.x).toInt, math.floor(origin.y).toInt, size)
    ensureColumnExists(here)

    val columnsToRemove = MutableSet.empty[ColumnRelWorld]
    val columnsToAdd = MutableSet.empty[ColumnRelWorld]
    for (col <- columnsAtEdge) {
      if (!inSight(col)) {
        columns.remove(col.value).get.unload()
        columnsToRemove += col
        for (offset <- ChunkColumn.neighbors) {
          val col2 = ColumnRelWorld(col.X + offset._1, col.Z + offset._2, size)
          if (columns.contains(col2.value)) {
            columnsToAdd += col2
          }
        }
      } else {
        var surrounded = true
        for (offset <- ChunkColumn.neighbors) {
          val col2 = ColumnRelWorld(col.X + offset._1, col.Z + offset._2, size)
          if (inSight(col2)) {
            if (!columns.contains(col2.value)) {
              columnsToAdd += col2
              columns(col2.value) = new ChunkColumn(col2, this)
            }
          } else {
            surrounded = false
          }
        }
        if (surrounded) columnsToRemove += col
      }
    }
    columnsAtEdge ++= columnsToAdd
    columnsAtEdge --= columnsToRemove
  }

  private def ensureColumnExists(here: ColumnRelWorld) = {
    if (!columns.contains(here.value)) {
      columns(here.value) = new ChunkColumn(here, this)
      columnsAtEdge += here
    }
  }

  def unload(): Unit = {
    val worldTag = NBTUtil.makeCompoundTag("world", Seq(
      NBTUtil.makeCompoundTag("general", Seq(
        new ByteTag("worldSize", size.worldSize.toByte),
        new StringTag("name", worldName)
      )),
      worldGenerator.toNBT,
      player.toNBT
    ))

    worldSettings.saveState(worldTag)

    chunksToLoad.clear
    loadColumnsCountdown = -1
    columns.values.foreach(_.unload())
    columns.clear
  }
}

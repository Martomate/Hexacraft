package hexacraft.world.storage

import java.io.{File, FileInputStream}
import java.util.Random

import com.flowpowered.nbt._
import com.flowpowered.nbt.stream.NBTInputStream
import hexacraft.Camera
import hexacraft.block.BlockState
import hexacraft.util.NBTUtil
import hexacraft.world.coord._
import hexacraft.world.gen.noise.{NoiseGenerator3D, NoiseGenerator4D}
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

class World(val saveDir: File, worldSettings: WorldSettings) {
  /* blockStorage
   * other world contents
   * methods for world interaction
   * method for loading/unloading blockColumns
   * block methods that will be forwarded to the correct chunk
   * worldGen references and methods
   * might rename this class to WorldStorage, and make another class World
   */

  val nbtData: CompoundTag = {
    val file = new File(saveDir, "world.dat")
    if (file.isFile) {
      val stream = new NBTInputStream(new FileInputStream(file))
      val nbt = stream.readTag().asInstanceOf[CompoundTag]
      stream.close()
      nbt
    }
    else {
      new CompoundTag("world", new CompoundMap())
    }
  }

  private val generalSettings: CompoundTag = nbtData.getValue.get("general").asInstanceOf[CompoundTag]
  private val worldGenSettings: CompoundTag = nbtData.getValue.get("gen").asInstanceOf[CompoundTag]

  val worldName: String = NBTUtil.getString(generalSettings, "worldName", worldSettings.name.getOrElse(saveDir.getName))
  val size: CylinderSize = new CylinderSize(NBTUtil.getByte(generalSettings, "worldSize", worldSettings.size.getOrElse(7)))

  val renderDistance: Double = 8 * CoordUtils.y60

  val columns = scala.collection.mutable.Map.empty[Long, ChunkColumn]
  val columnsAtEdge: MutableSet[ColumnRelWorld] = MutableSet.empty[ColumnRelWorld]

  private val randomGenSeed = NBTUtil.getLong(worldGenSettings, "seed", worldSettings.seed.getOrElse(new Random().nextLong))
  private val random = new Random(randomGenSeed)
  private[storage] val blockGenerator                = new NoiseGenerator4D(random, 8, NBTUtil.getDouble(worldGenSettings, "blockGenScale", 0.1))
  private[storage] val heightMapGenerator            = new NoiseGenerator3D(random, 8, NBTUtil.getDouble(worldGenSettings, "heightMapGenScale", 0.02))
  private[storage] val blockDensityGenerator         = new NoiseGenerator4D(random, 4, NBTUtil.getDouble(worldGenSettings, "blockDensityGenScale", 0.01))
  private[storage] val biomeHeightGenerator          = new NoiseGenerator3D(random, 4, NBTUtil.getDouble(worldGenSettings, "biomeHeightMapGenScale", 0.002))
  private[storage] val biomeHeightVariationGenerator = new NoiseGenerator3D(random, 4, NBTUtil.getDouble(worldGenSettings, "biomeHeightVariationGenScale", 0.002))

  private var loadColumnsCountdown = 0

  private[storage] var chunkLoadingOrigin: CylCoords = _
  private[storage] val chunkLoadingDirection: Vector3d = new Vector3d()
  private val chunksToLoad = mutable.PriorityQueue.empty[(Double, ChunkRelWorld)](Ordering.by(-_._1))
  // val chunkLoader = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))
  private val chunkRenderUpdateQueue = mutable.PriorityQueue.empty[(Double, ChunkRelWorld)](Ordering.by(-_._1))

  private val blocksToUpdate = mutable.Queue.empty[BlockRelWorld]

  val player = new Player(this)
  player.fromNBT(nbtData.getValue.get("player").asInstanceOf[CompoundTag])


  def addToBlockUpdateList(coords: BlockRelWorld): Unit = blocksToUpdate.enqueue(coords)

  def addRenderUpdate(coords: ChunkRelWorld): Unit = {
    chunkRenderUpdateQueue.enqueue(makeChunkToLoadTuple(coords))
  }

  def addChunkToLoadingQueue(coords: ChunkRelWorld): Unit = {
    chunksToLoad.enqueue(makeChunkToLoadTuple(coords))
  }

  private def makeChunkToLoadTuple(coords: ChunkRelWorld) = {
    val cyl = BlockCoords(coords.X * 16 + 8, coords.Y * 16 + 8, coords.Z * 16 + 8, size).toCylCoord
    val cDir = cyl.toNormalCoord(chunkLoadingOrigin).toVector3d.normalize()
    val dot = chunkLoadingDirection.dot(cDir)

    (chunkLoadingOrigin.distanceSq(cyl) * (1.25 - math.pow((dot + 1) / 2, 4)) / 1.25, coords)
  }

  def getColumn(coords: ColumnRelWorld): Option[ChunkColumn] = columns.get(coords.value)
  def getChunk(coords: ChunkRelWorld): Option[Chunk] =      getColumn(coords.getColumnRelWorld).flatMap(_.getChunk(coords.getChunkRelColumn))
  def getBlock(coords: BlockRelWorld): Option[BlockState] = getColumn(coords.getColumnRelWorld).flatMap(_.getBlock(coords.getBlockRelColumn))
  def setBlock(block: BlockState): Boolean =  getChunk(block.coords.getChunkRelWorld).fold(false)(_.setBlock(block))
  def removeBlock(coords: BlockRelWorld): Boolean = getChunk(coords.getChunkRelWorld).fold(false)(_.removeBlock(coords.getBlockRelChunk))
  def requestBlockUpdate(coords: BlockRelWorld): Unit = getChunk(coords.getChunkRelWorld).foreach(_.requestBlockUpdate(coords.getBlockRelChunk))

  def getHeight(x: Int, z: Int): Int = {
    val coords = ColumnRelWorld(x >> 4, z >> 4, size)
    getColumn(coords).getOrElse {
      val col = new ChunkColumn(coords, this)
      columns(coords.value) = col
      columnsAtEdge += coords
      col
    }.heightMap(x & 15)(z & 15)
  }

  def tick(camera: Camera): Unit = {
    chunkLoadingOrigin = CylCoords(player.position.x, player.position.y, player.position.z, size)
    val vec4 = new Vector4d(0, 0, -1, 0).mul(camera.invViewMatr)
    chunkLoadingDirection.set(vec4.x, vec4.y, vec4.z)// new Vector3d(0, 0, -1).rotateX(-player.rotation.x).rotateY(-player.rotation.y))

    if (loadColumnsCountdown == 0) {
      loadColumnsCountdown = World.ticksBetweenColumnLoading

      // TODO: this is a temporary placement
      val blocksToUpdateLen = blocksToUpdate.size
      for (_ <- 0 until blocksToUpdateLen) {
        val c = blocksToUpdate.dequeue()
        getChunk(c.getChunkRelWorld).foreach(_.doBlockUpdate(c.getBlockRelChunk))
      }

      val rDistSq = (renderDistance * 16) * (renderDistance * 16)

      //      chunksToLoad.enqueue(chunksToLoad.dequeueAll.map(c => makeChunkToLoadTuple(c._2)).filter(c => c._1 <= rDistSq): _*)
      chunksToLoad.clear()
      chunkRenderUpdateQueue.enqueue(chunkRenderUpdateQueue.dequeueAll.map(c => makeChunkToLoadTuple(c._2)).filter(c => c._1 <= rDistSq): _*)

      updateLoadedColumns()
      columns.values.foreach(_.updateLoadedChunks())
    }
    loadColumnsCountdown -= 1
    for (_ <- 1 to World.chunksLoadedPerTick) {
      if (chunksToLoad.nonEmpty) {
        val chunkToLoad = chunksToLoad.dequeue()._2
        getColumn(chunkToLoad.getColumnRelWorld).foreach(col => {
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
        })
      }
    }

    for (_ <- 1 to World.chunkRenderUpdatesPerTick) {
      if (chunkRenderUpdateQueue.nonEmpty) {
        val chunk = chunkRenderUpdateQueue.dequeue()._2
        getChunk(chunk).foreach(_.doRenderUpdate())
      }
    }

    columns.values.foreach(_.tick())
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
    if (!columns.contains(here.value)) {
      columns(here.value) = new ChunkColumn(here, this)
      columnsAtEdge += here
    }
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

  def unload(): Unit = {
    val worldTag = NBTUtil.makeCompoundTag("world", Seq(
      NBTUtil.makeCompoundTag("general", Seq(
        new ByteTag("worldSize", size.worldSize.toByte),
        new StringTag("name", worldName)
      )),
      NBTUtil.makeCompoundTag("gen", Seq(
        new LongTag("seed", randomGenSeed),
        new DoubleTag("blockGenScale", blockGenerator.scale),
        new DoubleTag("heightMapGenScale", heightMapGenerator.scale),
        new DoubleTag("blockDensityGenScale", blockDensityGenerator.scale),
        new DoubleTag("biomeHeightGenScale", biomeHeightGenerator.scale),
        new DoubleTag("biomeHeightVariationGenScale", biomeHeightVariationGenerator.scale)
      )),
      player.toNBT
    ))

    NBTUtil.saveTag(worldTag, new File(saveDir, "world.dat"))

    chunksToLoad.clear
    loadColumnsCountdown = -1
    columns.values.foreach(_.unload())
    columns.clear
  }
}

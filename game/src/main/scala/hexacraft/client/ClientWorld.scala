package hexacraft.client

import hexacraft.client.ClientWorld.WorldTickResult
import hexacraft.math.bits.Int12
import hexacraft.util.Result
import hexacraft.util.Result.{Err, Ok}
import hexacraft.world.*
import hexacraft.world.block.{Block, BlockRepository, BlockState}
import hexacraft.world.chunk.*
import hexacraft.world.coord.*
import hexacraft.world.entity.{Entity, EntityFactory, EntityPhysicsSystem}

import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.annotation.tailrec
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

object ClientWorld {
  class WorldTickResult(
      val chunksNeedingRenderUpdate: Seq[ChunkRelWorld]
  )
}

class ClientWorld(val worldInfo: WorldInfo) extends BlockRepository with BlocksInWorld {
  given size: CylinderSize = worldInfo.worldSize

  private val backgroundTasks: mutable.ArrayBuffer[Future[Unit]] = mutable.ArrayBuffer.empty

  private val columns: mutable.LongMap[ChunkColumnTerrain] = mutable.LongMap.empty
  private val chunks: mutable.LongMap[Chunk] = mutable.LongMap.empty

  val worldGenerator = new WorldGenerator(worldInfo.gen)

  private val chunksNeedingRenderUpdate = mutable.ArrayBuffer.empty[ChunkRelWorld]
  private val lightPropagator: LightPropagator = new LightPropagator(this, this.requestRenderUpdate)

  val renderDistance: Double = 8 * CylinderSize.y60

  val collisionDetector = new CollisionDetector(this)
  private val entityPhysicsSystem = EntityPhysicsSystem(this, collisionDetector)

  private val entitiesToSpawnLater = mutable.ArrayBuffer.empty[Entity]

  def getColumn(coords: ColumnRelWorld): Option[ChunkColumnTerrain] = {
    columns.get(coords.value)
  }

  def getChunk(coords: ChunkRelWorld): Option[Chunk] = {
    chunks.get(coords.value)
  }

  def loadedChunks: Seq[ChunkRelWorld] = {
    chunks.keys.map(c => ChunkRelWorld(c)).toSeq
  }

  def getBlock(coords: BlockRelWorld): BlockState = {
    getChunk(coords.getChunkRelWorld) match {
      case Some(chunk) => chunk.getBlock(coords.getBlockRelChunk)
      case None        => BlockState.Air
    }
  }

  def setBlock(coords: BlockRelWorld, block: BlockState): Unit = {
    getChunk(coords.getChunkRelWorld) match {
      case Some(chunk) =>
        chunk.setBlock(coords.getBlockRelChunk, block)
        onSetBlock(coords, block)
      case None =>
    }
  }

  def removeBlock(coords: BlockRelWorld): Unit = {
    getChunk(coords.getChunkRelWorld) match {
      case Some(chunk) =>
        chunk.setBlock(coords.getBlockRelChunk, BlockState.Air)
        onSetBlock(coords, BlockState.Air)
      case None =>
    }
  }

  def addEntity(entity: Entity): Option[ChunkRelWorld] = {
    val chunkCoords = chunkOfEntity(entity)
    getChunk(chunkCoords) match {
      case Some(chunk) =>
        chunk.addEntity(entity)
        Some(chunkCoords)
      case None =>
        None
    }
  }

  def removeEntity(entity: Entity): Unit = {
    chunks.values.find(_.entities.exists(_.id == entity.id)) match {
      case Some(chunk) =>
        chunk.removeEntity(entity)
      case None =>
    }
  }

  def removeAllEntities(): Unit = {
    for {
      ch <- chunks.values
      e <- ch.entities.toSeq
    } do {
      ch.removeEntity(e)
    }
  }

  private def chunkOfEntity(entity: Entity): ChunkRelWorld = {
    CoordUtils.approximateChunkCoords(entity.transform.position)
  }

  def getHeight(x: Int, z: Int): Option[Int] = {
    val coords = ColumnRelWorld(x >> 4, z >> 4)
    getColumn(coords).map(_.terrainHeight.getHeight(x & 15, z & 15))
  }

  def setColumn(coords: ColumnRelWorld, column: ChunkColumnTerrain): Unit = {
    columns(coords.value) = column
  }

  def setChunk(chunkCoords: ChunkRelWorld, ch: Chunk): Unit = {
    getColumn(chunkCoords.getColumnRelWorld) match {
      case Some(col) =>
        setChunkAndUpdateHeightmap(col, chunkCoords, ch)
        updateHeightmapAfterChunkUpdate(col, chunkCoords, ch)
      case None =>
    }

    ch.initLightingIfNeeded(chunkCoords, lightPropagator)

    requestRenderUpdate(chunkCoords)
    requestRenderUpdateForNeighborChunks(chunkCoords)
  }

  private def updateHeightmapAfterChunkUpdate(col: ChunkColumnTerrain, chunkCoords: ChunkRelWorld, chunk: Chunk)(using
      CylinderSize
  ): Unit = {
    for {
      cx <- 0 until 16
      cz <- 0 until 16
    } do {
      val blockCoords = BlockRelChunk(cx, 15, cz)
      updateHeightmapAfterBlockUpdate(
        col,
        BlockRelWorld.fromChunk(blockCoords, chunkCoords),
        chunk.getBlock(blockCoords)
      )
    }
  }

  private def updateHeightmapAfterBlockUpdate(col: ChunkColumnTerrain, coords: BlockRelWorld, now: BlockState): Unit = {
    val height = col.terrainHeight.getHeight(coords.cx, coords.cz)

    if coords.y >= height then {
      if now.blockType != Block.Air then {
        col.terrainHeight.setHeight(coords.cx, coords.cz, coords.y.toShort)
      } else {
        val newHeight = LazyList
          .range((height - 1).toShort, Short.MinValue, -1.toShort)
          .map(y =>
            chunks
              .get(ChunkRelWorld(coords.X.toInt, y >> 4, coords.Z.toInt).value)
              .map(chunk => (y, chunk.getBlock(BlockRelChunk(coords.cx, y & 15, coords.cz))))
              .orNull
          )
          .takeWhile(_ != null) // stop searching if the chunk is not loaded
          .collectFirst({ case (y, block) if block.blockType != Block.Air => y })
          .getOrElse(Short.MinValue)

        col.terrainHeight.setHeight(coords.cx, coords.cz, newHeight)
      }
    }
  }

  private def updateHeightmapAfterChunkReplaced(
      heightMap: ChunkColumnHeightMap,
      chunkCoords: ChunkRelWorld,
      chunk: Chunk
  ): Unit = {
    val yy = chunkCoords.Y.toInt * 16
    for x <- 0 until 16 do {
      for z <- 0 until 16 do {
        val height = heightMap.getHeight(x, z)

        val highestBlockY = (yy + 15 to yy by -1)
          .filter(_ > height)
          .find(y => chunk.getBlock(BlockRelChunk(x, y, z)).blockType != Block.Air)

        highestBlockY match {
          case Some(h) => heightMap.setHeight(x, z, h.toShort)
          case None    =>
        }
      }
    }
  }

  private def setChunkAndUpdateHeightmap(col: ChunkColumnTerrain, chunkCoords: ChunkRelWorld, chunk: Chunk): Unit = {
    chunks.put(chunkCoords.value, chunk) match {
      case Some(`chunk`)  => // the chunk is not new so nothing needs to be done
      case Some(oldChunk) => updateHeightmapAfterChunkReplaced(col.terrainHeight, chunkCoords, chunk)
      case None           => updateHeightmapAfterChunkReplaced(col.terrainHeight, chunkCoords, chunk)
    }
  }

  def removeChunk(chunkCoords: ChunkRelWorld): Boolean = {
    val columnCoords = chunkCoords.getColumnRelWorld

    var chunkWasRemoved = false

    for col <- columns.get(columnCoords.value) do {
      for removedChunk <- chunks.remove(chunkCoords.value) do {
        chunkWasRemoved = true
        requestRenderUpdate(chunkCoords) // this will remove the render data for the chunk
        requestRenderUpdateForNeighborChunks(chunkCoords)
      }

      if chunks.keys.count(v => ChunkRelWorld(v).getColumnRelWorld == columnCoords) == 0 then {
        columns.remove(columnCoords.value)
      }
    }

    chunkWasRemoved
  }

  def tick(cameras: Seq[Camera], entityEvents: Seq[(UUID, EntityEvent)]): WorldTickResult = {
    val allEntitiesById = mutable.HashMap.empty[UUID, (ChunkRelWorld, Entity)]
    for (chunkCoordsValue, ch) <- chunks do {
      val chunkCoords = ChunkRelWorld(chunkCoordsValue)
      for e <- ch.entities do {
        allEntitiesById(e.id) = (chunkCoords, e)
      }
    }

    {
      val entities = entitiesToSpawnLater.toSeq
      entitiesToSpawnLater.clear()
      for e <- entities do {
        if addEntity(e).isEmpty then {
          entitiesToSpawnLater += e
        }
      }
    }

    for (id, event) <- entityEvents do {
      allEntitiesById.get(id) match {
        case Some(ent) =>
          val (c, e) = ent
          event match {
            case EntityEvent.Spawned(_) =>
              println(s"Received spawn event for an entity that already exists (id: $id)")
            case EntityEvent.Despawned =>
              removeEntity(e)
              allEntitiesById -= id
              println(s"Client: despawned entity $id")
            case EntityEvent.Position(pos) =>
              e.transform.position = pos
            case EntityEvent.Rotation(r) =>
              e.transform.rotation.set(r)
            case EntityEvent.Velocity(v) =>
              e.velocity.velocity.set(v)
          }
        case None =>
          event match {
            case EntityEvent.Spawned(data) =>
              val entity = for {
                e <- EntityFactory
                  .fromNbt(data)
                  .mapErr(err => s"Could not create entity. Error: $err")
              } yield e

              entity match {
                case Ok(e) =>
                  addEntity(e) match {
                    case Some(c) =>
                      allEntitiesById(id) = (c, e)
                      println(s"Client: spawned entity $id")
                    case None =>
                      entitiesToSpawnLater += e
                  }
                case Err(e) =>
                  println(e)
              }
            case _ =>
              println(s"Received entity event for an unknown entity (id: $id, event: $event)")
          }
      }
    }

    for ch <- chunks.values do {
      ch.optimizeStorage()
      tickEntities(ch.entities)
    }

    val r = chunksNeedingRenderUpdate.toSeq
    chunksNeedingRenderUpdate.clear()

    new WorldTickResult(r)
  }

  @tailrec // this is done for performance
  private def tickEntities(ents: Iterable[Entity]): Unit = {
    if ents.nonEmpty then {
      tickEntity(ents.head)
      tickEntities(ents.tail)
    }
  }

  private def tickEntity(e: Entity): Unit = {
    e.ai match {
      case Some(ai) =>
        ai.tick(this, e.transform, e.velocity, e.boundingBox)
        e.velocity.velocity.add(ai.acceleration)
      case None =>
    }

    e.velocity.velocity.x *= 0.9
    e.velocity.velocity.z *= 0.9

    entityPhysicsSystem.update(e.transform, e.velocity, e.boundingBox)

    e.model.foreach(_.tick(e.velocity.velocity.lengthSquared() > 0.1))
  }

  private def requestRenderUpdateForNeighborChunks(coords: ChunkRelWorld): Unit = {
    for side <- 0 until 8 do {
      val nCoords = coords.offset(NeighborOffsets(side))
      if getChunk(nCoords).isDefined then {
        requestRenderUpdate(nCoords)
      }
    }
  }

  def getBrightness(block: BlockRelWorld): Float = {
    getChunk(block.getChunkRelWorld) match {
      case Some(c) => c.lighting.getBrightness(block.getBlockRelChunk)
      case None    => 1.0f
    }
  }

  private def requestRenderUpdate(chunkCoords: ChunkRelWorld): Unit = {
    chunksNeedingRenderUpdate += chunkCoords
  }

  def unload(): Unit = {
    chunks.clear()
    columns.clear()

    for t <- backgroundTasks do {
      Await.result(t, Duration(10, TimeUnit.SECONDS))
    }
  }

  private def onSetBlock(coords: BlockRelWorld, block: BlockState): Unit = {
    def affectedChunkOffset(where: Byte): Int = {
      where match {
        case 0  => -1
        case 15 => 1
        case _  => 0
      }
    }

    def isInNeighborChunk(chunkOffset: Offset) = {
      val xx = affectedChunkOffset(coords.cx)
      val yy = affectedChunkOffset(coords.cy)
      val zz = affectedChunkOffset(coords.cz)

      chunkOffset.dx * xx == 1 || chunkOffset.dy * yy == 1 || chunkOffset.dz * zz == 1
    }

    for col <- columns.get(coords.getColumnRelWorld.value) do {
      updateHeightmapAfterBlockUpdate(col, coords, block)
    }

    val cCoords = coords.getChunkRelWorld
    val bCoords = coords.getBlockRelChunk

    for c <- getChunk(cCoords) do {
      handleLightingOnSetBlock(cCoords, c, bCoords, block)
      requestRenderUpdate(cCoords)

      for s <- 0 until 8 do {
        val neighCoords = bCoords.globalNeighbor(s, cCoords)
        val neighChunkCoords = neighCoords.getChunkRelWorld

        if neighChunkCoords != cCoords then {
          for n <- getChunk(neighChunkCoords) do {
            requestRenderUpdate(neighChunkCoords)
          }
        }
      }
    }
  }

  private def handleLightingOnSetBlock(
      chunkCoords: ChunkRelWorld,
      chunk: Chunk,
      blockCoords: BlockRelChunk,
      block: BlockState
  ): Unit = {
    lightPropagator.removeTorchlight(chunkCoords, chunk, blockCoords)
    lightPropagator.removeSunlight(chunkCoords, chunk, blockCoords)
    if block.blockType.lightEmitted != 0 then {
      lightPropagator.addTorchlight(chunkCoords, chunk, blockCoords, block.blockType.lightEmitted)
    }
  }
}

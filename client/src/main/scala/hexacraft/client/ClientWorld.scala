package hexacraft.client

import hexacraft.client.ClientWorld.WorldTickResult
import hexacraft.math.bits.Int12
import hexacraft.util.{Loop, Result}
import hexacraft.util.Result.{Err, Ok}
import hexacraft.world.*
import hexacraft.world.block.{Block, BlockRepository, BlockState}
import hexacraft.world.chunk.*
import hexacraft.world.coord.*
import hexacraft.world.entity.{Entity, EntityFactory, EntityPhysicsSystem}

import java.util.UUID
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object ClientWorld {
  class WorldTickResult(
      val chunksNeedingRenderUpdate: Seq[ChunkRelWorld]
  )
}

class ClientWorld(val worldInfo: WorldInfo) extends BlockRepository with BlocksInWorld {
  given size: CylinderSize = worldInfo.worldSize

  private val columns: mutable.LongMap[ChunkColumnTerrain] = mutable.LongMap.empty
  private val chunks: mutable.LongMap[Chunk] = mutable.LongMap.empty
  private val chunkList: mutable.ArrayBuffer[Chunk] = mutable.ArrayBuffer.empty

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

  inline def foreachChunk(inline f: Chunk => Unit): Unit = {
    Loop.array(chunkList)(f)
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
    chunkList.find(_.entities.exists(_.id == entity.id)) match {
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

    lightPropagator.synchronized {
      ch.initLightingIfNeeded(chunkCoords, lightPropagator)
    }

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
      case Some(`chunk`) => // the chunk is not new so nothing needs to be done
      case Some(oldChunk) =>
        val oldIdx = this.chunkList.indexOfRef(oldChunk)
        this.chunkList(oldIdx) = chunk
        updateHeightmapAfterChunkReplaced(col.terrainHeight, chunkCoords, chunk)
      case None =>
        chunkList += chunk
        updateHeightmapAfterChunkReplaced(col.terrainHeight, chunkCoords, chunk)
    }
  }

  extension [A <: AnyRef](arr: mutable.ArrayBuffer[A]) {
    private def indexOfRef(elem: A): Int = {
      Loop.rangeUntil(0, arr.length) { i =>
        if arr(i).eq(elem) then return i
      }
      -1
    }
  }

  def removeChunk(chunkCoords: ChunkRelWorld): Boolean = {
    val columnCoords = chunkCoords.getColumnRelWorld

    var chunkWasRemoved = false

    columns.get(columnCoords.value) match {
      case Some(col) =>
        chunks.remove(chunkCoords.value) match {
          case Some(removedChunk) =>
            {
              // remove `removedChunk` from `chunkList` by replacing it with the last element
              val dst = this.chunkList.indexOf(removedChunk)
              val src = this.chunkList.size - 1
              val removed = this.chunkList.remove(src)
              if dst != src then {
                this.chunkList(dst) = removed
              }
            }

            chunkWasRemoved = true
            requestRenderUpdate(chunkCoords) // this will remove the render data for the chunk
            requestRenderUpdateForNeighborChunks(chunkCoords)
          case None =>
        }

        if chunks.keys.count(v => ChunkRelWorld(v).getColumnRelWorld == columnCoords) == 0 then {
          columns.remove(columnCoords.value)
        }
      case None =>
    }

    chunkWasRemoved
  }

  def tick(cameras: Seq[Camera], entityEvents: Seq[(UUID, EntityEvent)]): WorldTickResult = {
    val allEntitiesById = mutable.HashMap.empty[UUID, (Option[ChunkRelWorld], Entity)]

    {
      val chIt = chunks.iterator
      while chIt.hasNext do {
        val (chunkCoordsValue, ch) = chIt.next()
        val chunkCoords = ChunkRelWorld(chunkCoordsValue)
        val eIt = ch.entities.iterator
        while eIt.hasNext do {
          val e = eIt.next()
          allEntitiesById(e.id) = (Some(chunkCoords), e)
        }
      }
    }

    {
      val entities = entitiesToSpawnLater.toSeq
      entitiesToSpawnLater.clear()
      val eIt = entities.iterator
      while eIt.hasNext do {
        val e = eIt.next()
        if addEntity(e).isEmpty then {
          allEntitiesById(e.id) = (None, e)
          entitiesToSpawnLater += e
          // println(s"Client: not ready to spawn entity ${e.id}")
        } else {
          println(s"Client: finally spawned entity ${e.id}")
        }
      }
    }

    val evIt = entityEvents.iterator
    while evIt.hasNext do {
      val (id, event) = evIt.next()
      allEntitiesById.get(id) match {
        case Some(ent) =>
          val (_, e) = ent
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
              e.motion.velocity.set(v)
            case EntityEvent.Flying(f) =>
              e.motion.flying = f
            case EntityEvent.HeadDirection(d) =>
              e.headDirection.foreach(_.direction = d)
          }
        case None =>
          event match {
            case EntityEvent.Spawned(data) =>
              val entity = EntityFactory
                .fromNbt(data)
                .mapErr(err => s"Could not create entity. Error: $err")

              entity match {
                case Ok(e) =>
                  addEntity(e) match {
                    case Some(c) =>
                      allEntitiesById(id) = (Some(c), e)
                      println(s"Client: spawned entity $id")
                    case None =>
                      allEntitiesById(id) = (None, e)
                      entitiesToSpawnLater += e
                  }
                case Err(e) =>
                  println(e)
              }
            case _ =>
            // println(s"Received entity event for an unknown entity (id: $id, event: $event)")
          }
      }
    }

    {
      val chIt = chunkList.iterator
      while chIt.hasNext do {
        val ch = chIt.next()
        ch.optimizeStorage()

        val eIt = ch.entities.iterator
        while eIt.hasNext do {
          val e = eIt.next()
          tickEntity(e)
        }
      }
    }

    val r = chunksNeedingRenderUpdate.toSeq
    chunksNeedingRenderUpdate.clear()

    new WorldTickResult(r)
  }

  private def tickEntity(e: Entity): Unit = {
    e.ai match {
      case Some(ai) =>
        ai.tick(this, e.transform, e.motion, e.boundingBox)
        e.motion.velocity.add(ai.acceleration)
      case None =>
    }

    e.motion.velocity.x *= 0.9
    e.motion.velocity.z *= 0.9

    entityPhysicsSystem.update(e.transform, e.motion, e.boundingBox)

    val vel = e.motion.velocity
    val horizontalSpeedSq = vel.x * vel.x + vel.z * vel.z
    if e.model.isDefined then {
      e.model.get.tick(horizontalSpeedSq > 0.1, e.headDirection.map(_.direction))
    }
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
      case Some(c) => c.getBrightness(block.getBlockRelChunk)
      case None    => 1.0f
    }
  }

  private def requestRenderUpdate(chunkCoords: ChunkRelWorld): Unit = {
    chunksNeedingRenderUpdate += chunkCoords
  }

  def unload(): Unit = {
    chunkList.clear()
    chunks.clear()
    columns.clear()
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
    lightPropagator.synchronized {
      lightPropagator.removeTorchlight(chunkCoords, chunk, blockCoords)
      lightPropagator.removeSunlight(chunkCoords, chunk, blockCoords)
      if block.blockType.lightEmitted != 0 then {
        lightPropagator.addTorchlight(chunkCoords, chunk, blockCoords, block.blockType.lightEmitted)
      }
    }
  }
}

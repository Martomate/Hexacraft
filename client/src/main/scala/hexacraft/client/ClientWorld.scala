package hexacraft.client

import hexacraft.client.ClientWorld.WorldTickResult
import hexacraft.math.bits.Int12
import hexacraft.nbt.Nbt
import hexacraft.util.Loop
import hexacraft.world.*
import hexacraft.world.block.{Block, BlockRepository, BlockState}
import hexacraft.world.chunk.*
import hexacraft.world.coord.*
import hexacraft.world.entity.Entity

import java.util.UUID
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object ClientWorld {
  class WorldTickResult(
      val chunksNeedingRenderUpdate: Seq[ChunkRelWorld]
  )
}

class ClientWorld(val worldInfo: WorldInfo, val renderDistance: Double) extends BlockRepository with BlocksInWorld {
  given size: CylinderSize = worldInfo.worldSize

  private val columns: mutable.LongMap[ChunkColumnHeightMap] = mutable.LongMap.empty
  private val chunks: mutable.LongMap[Chunk] = mutable.LongMap.empty
  private val chunkList: mutable.ArrayBuffer[Chunk] = mutable.ArrayBuffer.empty

  private val chunksNeedingRenderUpdate = mutable.ArrayBuffer.empty[ChunkRelWorld]
  private val lightPropagator: LightPropagator = new LightPropagator(this, this.requestRenderUpdate)

  val collisionDetector = new CollisionDetector(this)

  /** The entities are loaded separately from the chunks, so they are stored here rather than in the chunks */
  private val entities = mutable.ArrayBuffer.empty[Entity]

  def getColumn(coords: ColumnRelWorld): Option[ChunkColumnHeightMap] = {
    columns.get(coords.value)
  }

  def getChunk(coords: ChunkRelWorld): Option[Chunk] = {
    chunks.get(coords.value)
  }

  inline def foreachEntity(inline f: Entity => Unit): Unit = {
    Loop.array(entities)(f)
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

  private def addEntity(entity: Entity): Unit = {
    entities += entity
  }

  private def removeEntity(entity: Entity): Unit = {
    val idx = entities.indexWhere(_.id == entity.id)
    if idx != -1 then {
      // remove the entity by replacing it with the last element
      val last = entities.remove(entities.size - 1)
      if idx != entities.size then {
        entities(idx) = last
      }
    }
  }

  def getHeight(x: Int, z: Int): Option[Int] = {
    val coords = ColumnRelWorld(x >> 4, z >> 4)
    columns.get(coords.value).map(_.getHeight(x & 15, z & 15))
  }

  def setColumn(coords: ColumnRelWorld, column: ChunkColumnHeightMap): Unit = {
    columns(coords.value) = column
  }

  def setChunk(chunkCoords: ChunkRelWorld, ch: Chunk): Unit = {
    val col = columns(chunkCoords.getColumnRelWorld.value)
    setChunkAndUpdateHeightmap(col, chunkCoords, ch)
    updateHeightmapAfterChunkUpdate(col, chunkCoords, ch)

    lightPropagator.synchronized {
      ch.initLightingIfNeeded(chunkCoords, lightPropagator)
    }

    requestRenderUpdate(chunkCoords)
    requestRenderUpdateForNeighborChunks(chunkCoords)
  }

  private def updateHeightmapAfterChunkUpdate(col: ChunkColumnHeightMap, chunkCoords: ChunkRelWorld, chunk: Chunk)(using
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

  private def updateHeightmapAfterBlockUpdate(
      col: ChunkColumnHeightMap,
      coords: BlockRelWorld,
      now: BlockState
  ): Unit = {
    val height = col.getHeight(coords.cx, coords.cz)

    if coords.y >= height then {
      if now.blockType != Block.Air then {
        col.setHeight(coords.cx, coords.cz, coords.y.toShort)
      } else {
        col.recalculate(
          coords,
          Y => this.chunks.get(ChunkRelWorld(coords.X.toInt, Y, coords.Z.toInt).value)
        )
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

  private def setChunkAndUpdateHeightmap(col: ChunkColumnHeightMap, chunkCoords: ChunkRelWorld, chunk: Chunk): Unit = {
    chunks.put(chunkCoords.value, chunk) match {
      case Some(`chunk`) => // the chunk is not new so nothing needs to be done
      case Some(oldChunk) =>
        val oldIdx = this.chunkList.indexOfRef(oldChunk)
        this.chunkList(oldIdx) = chunk
        updateHeightmapAfterChunkReplaced(col, chunkCoords, chunk)
      case None =>
        chunkList += chunk
        updateHeightmapAfterChunkReplaced(col, chunkCoords, chunk)
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

    chunkWasRemoved
  }

  def tick(cameras: Seq[Camera], entityEvents: Seq[(UUID, EntityEvent)]): WorldTickResult = {
    val allEntitiesById = mutable.HashMap.empty[UUID, Entity]
    Loop.array(entities) { e =>
      allEntitiesById(e.id) = e
    }

    Loop.iterate(entityEvents.iterator) { case (id, event) =>
      allEntitiesById.get(id) match {
        case Some(e) =>
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
              Entity.decodeWithoutAi(data).map(EntityModels.addModel) match {
                case Some(e) =>
                  addEntity(e)
                  allEntitiesById(id) = e
                  println(s"Client: spawned entity $id")
                case None =>
                  println(s"Could not create entity")
              }
            case _ =>
            // println(s"Received entity event for an unknown entity (id: $id, event: $event)")
          }
      }
    }

    Loop.array(chunkList) { ch =>
      ch.optimizeStorage()
    }

    Loop.array(entities) { e =>
      tickEntity(e)
    }

    val r = chunksNeedingRenderUpdate.toSeq
    chunksNeedingRenderUpdate.clear()

    new WorldTickResult(r)
  }

  private def tickEntity(e: Entity): Unit = {
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
    entities.clear()
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

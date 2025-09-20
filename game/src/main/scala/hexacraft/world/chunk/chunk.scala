package hexacraft.world.chunk

import hexacraft.nbt.{Nbt, NbtCodec}
import hexacraft.util.Loop
import hexacraft.world.*
import hexacraft.world.block.BlockState
import hexacraft.world.coord.{BlockRelChunk, ChunkRelWorld}
import hexacraft.world.entity.Entity

import scala.collection.immutable.ArraySeq
import scala.collection.mutable

object Chunk {
  def fromGenerator(coords: ChunkRelWorld, column: ChunkColumnTerrain, generator: WorldGenerator)(using
      CylinderSize
  ) = {
    new Chunk(ChunkData.fromStorage(generator.generateChunk(coords, column)))
  }

  given (using CylinderSize): NbtCodec[Chunk] with {
    override def encode(chunk: Chunk): Nbt.MapTag = {
      Nbt.encode(chunk.chunkData)
    }

    override def decode(tag: Nbt.MapTag): Option[Chunk] = {
      Some(new Chunk(Nbt.decode[ChunkData](tag).get))
    }
  }
}

final class Chunk private (private val chunkData: ChunkData)(using CylinderSize) {
  private var _modCount: Long = 0L
  private var _hasEntities: Boolean = chunkData.entities.nonEmpty

  def modCount: Long = _modCount

  private val sunlight: Array[Byte] = new Array[Byte](16 * 16 * 16)
  private val torchlight: Array[Byte] = new Array[Byte](16 * 16 * 16)
  private var brightnessInitialized: Boolean = false

  inline def setSunlight(coords: BlockRelChunk, value: Byte): Unit = {
    sunlight(coords.value) = value
  }

  inline def getSunlight(coords: BlockRelChunk): Byte = {
    sunlight(coords.value)
  }

  inline def setTorchlight(coords: BlockRelChunk, value: Byte): Unit = {
    torchlight(coords.value) = value
  }

  inline def getTorchlight(coords: BlockRelChunk): Byte = {
    torchlight(coords.value)
  }

  def getBrightness(block: BlockRelChunk): Float = {
    math.min((torchlight(block.value) + sunlight(block.value)) / 15f, 1.0f)
  }

  def initLightingIfNeeded(coords: ChunkRelWorld, lightPropagator: LightPropagator): Unit = {
    if !brightnessInitialized then {
      brightnessInitialized = true
      lightPropagator.initBrightnesses(coords, this)
    }
  }

  def entities: collection.IndexedSeq[Entity] = chunkData.entities

  inline def foreachEntity(inline f: Entity => Unit): Unit = {
    Loop.array(chunkData.entities)(f)
  }

  def hasEntities: Boolean = {
    _hasEntities
  }

  def addEntity(entity: Entity): Unit = {
    chunkData.entities += entity
    _hasEntities = true
    _modCount += 1
  }

  def removeEntity(entity: Entity): Unit = {
    chunkData.entities -= entity
    if chunkData.entities.isEmpty then {
      _hasEntities = false
    }
    _modCount += 1
  }

  def blocks: Array[LocalBlockState] = chunkData.storage.allBlocks

  def getBlock(coords: BlockRelChunk): BlockState = chunkData.storage.getBlock(coords)

  def setBlock(blockCoords: BlockRelChunk, block: BlockState): Unit = {
    val before = getBlock(blockCoords)
    if before != block then {
      chunkData.storage.setBlock(blockCoords, block)
      _modCount += 1
    }
  }

  def optimizeStorage(): Unit = {
    chunkData.optimizeStorage()
  }

  def isDecorated: Boolean = chunkData.isDecorated

  def setDecorated(): Unit = {
    if !chunkData.isDecorated then {
      chunkData.isDecorated = true
      _modCount += 1
    }
  }
}

class ChunkData(
    private[chunk] var storage: ChunkStorage,
    private[chunk] val entities: mutable.ArrayBuffer[Entity],
    private[chunk] var isDecorated: Boolean
) {
  def optimizeStorage(): Unit = {
    if storage.isDense then {
      if storage.numBlocks < 32 then {
        storage = SparseChunkStorage.fromStorage(storage)
      }
    } else {
      if storage.numBlocks > 48 then {
        storage = DenseChunkStorage.fromStorage(storage)
      }
    }
  }
}

object ChunkData {
  def fromStorage(storage: ChunkStorage): ChunkData = {
    new ChunkData(storage, mutable.ArrayBuffer.empty, false)
  }

  given (using CylinderSize): NbtCodec[ChunkData] with {
    def encode(value: ChunkData): Nbt.MapTag = {
      val storageNbt = value.storage.toNBT

      Nbt.makeMap(
        "blocks" -> Nbt.ByteArrayTag.of(storageNbt.blocks),
        "metadata" -> Nbt.ByteArrayTag.of(storageNbt.metadata),
        "entities" -> Nbt.ListTag(value.entities.map(e => Nbt.encode(e)).toSeq),
        "isDecorated" -> Nbt.ByteTag(value.isDecorated)
      )
    }

    def decode(nbt: Nbt.MapTag): Option[ChunkData] = {
      val storage = nbt.getByteArray("blocks").map(_.unsafeArray) match {
        case Some(blocks) =>
          val numBlocks = blocks.count(_ != 0)
          val meta = nbt
            .getByteArray("metadata")
            .map(_.unsafeArray)
            .getOrElse(Array.fill(16 * 16 * 16)(0.toByte))

          if numBlocks < 32 then {
            SparseChunkStorage.create(blocks, meta)
          } else {
            DenseChunkStorage.create(blocks, meta)
          }
        case None =>
          SparseChunkStorage.empty
      }

      val entities = mutable.ArrayBuffer.empty[Entity]
      for {
        tags <- nbt.getList("entities")
        tag <- tags.map(_.asInstanceOf[Nbt.MapTag])
      } do {
        Nbt.decode[Entity](tag) match {
          case Some(entity) => entities += entity
          case None         => println(s"Could not load entity")
        }
      }

      val isDecorated = nbt.getBoolean("isDecorated", default = false)

      Some(new ChunkData(storage, entities, isDecorated))
    }
  }
}

package com.martomate.hexacraft.world.storage

import com.flowpowered.nbt.ShortArrayTag
import com.martomate.hexacraft.block.{Block, BlockAir, BlockState}
import com.martomate.hexacraft.util.NBTUtil
import com.martomate.hexacraft.world.coord.{BlockRelChunk, BlockRelColumn, ChunkRelColumn, ColumnRelWorld}
import org.joml.Vector2d

object ChunkColumn {
  val neighbors: Seq[(Int, Int)] = Seq(
    (1, 0),
    (0, 1),
    (-1, 0),
    (0, -1))
}

class ChunkColumn(val coords: ColumnRelWorld, val world: World) {
  val chunks = scala.collection.mutable.Map.empty[Int, Chunk]
  private[storage] var topAndBottomChunks: Option[(Int, Int)] = None

  private[storage] val generatedHeightMap = {
    val interp = world.worldGenerator.getHeightmapInterpolator(coords)

    for (x <- 0 until 16) yield {
      for (z <- 0 until 16) yield {
        interp(x, z).toShort
      }
    }
  }

  private def saveFilePath: String = "data/" + coords.value + "/column.dat"

  private val _heightMap: IndexedSeq[Array[Short]] = {
    val columnNBT = world.worldSettings.loadState(saveFilePath)
    NBTUtil.getShortArray(columnNBT, "heightMap") match {
      case Some(heightNBT) =>
        for (x <- 0 until 16) yield Array.tabulate(16)(z => heightNBT.array((x << 4) | z))
      case None =>
        for (x <- 0 until 16) yield Array.tabulate(16)(z => generatedHeightMap(x)(z))
    }
  }

  def heightMap(x: Int, z: Int): Short = {
    _heightMap(x)(z)
  }

  def getChunk(coords: ChunkRelColumn): Option[Chunk] = chunks.get(coords.value)

  def getBlock(coords: BlockRelColumn): BlockState = {
    getChunk(coords.getChunkRelColumn).map(_.getBlock(coords.getBlockRelChunk)).getOrElse(BlockAir.State)
  }

  def onSetBlock(coords: BlockRelColumn, block: BlockState): Unit = {
    val height = heightMap(coords.cx, coords.cz)
    if (block.blockType == Block.Air) {
      if (coords.y == height) {
        // remove and find the next highest
        var y: Int = height
        var ch: Option[Chunk] = None
        do {
          y -= 1
          ch = getChunk(ChunkRelColumn(y >> 4, coords.cylSize))
          ch match {
            case Some(chunk) =>
              if (chunk.getBlock(BlockRelChunk(coords.cx, y & 0xf, coords.cz, coords.cylSize)).blockType != Block.Air)
                ch = None
              else
                y -= 1
            case None =>
              y = Short.MinValue
          }//.filter(_.getBlock(BlockRelChunk(coords.cx, y & 0xf, coords.cz, coords.cylSize)).blockType != Block.Air)
        } while (ch.isDefined)

        _heightMap(coords.cx)(coords.cz) = y.toShort
      }
    } else {
      if (coords.y > height) {
        _heightMap(coords.cx)(coords.cz) = coords.y.toShort
      }
    }
  }

  def onChunkLoaded(chunk: Chunk): Unit = {
    val yy = chunk.coords.Y * 16
    for (x <- 0 until 16) {
      for (z <- 0 until 16) {
        val height = heightMap(x, z)
        (yy + 15 to yy by -1).filter(_ > height).find(y =>
          chunk.getBlock(BlockRelChunk(x, y, z, chunk.coords.cylSize)).blockType != Block.Air
        ).foreach(h => {
          _heightMap(x)(z) = h.toShort
        })
      }
    }
  }

  def tick(): Unit = {
    chunks.values.foreach(_.tick())
  }

  def unload(): Unit = {
    chunks.values.foreach{c =>
      world.chunkAddedOrRemovedListeners.foreach(_.onChunkRemoved(c))
      c.unload()
    }

    world.worldSettings.saveState(NBTUtil.makeCompoundTag("column", Seq(
      new ShortArrayTag("heightMap", Array.tabulate(16*16)(i => heightMap(i >> 4, i & 0xf)))
    )), saveFilePath)
  }
}

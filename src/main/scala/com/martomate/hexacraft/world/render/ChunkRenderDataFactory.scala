package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.world.{BlocksInWorld, ChunkCache, CylinderSize}
import com.martomate.hexacraft.world.block.Blocks
import com.martomate.hexacraft.world.chunk.Chunk
import com.martomate.hexacraft.world.chunk.storage.LocalBlockState
import com.martomate.hexacraft.world.coord.CoordUtils
import com.martomate.hexacraft.world.coord.fp.CylCoords
import com.martomate.hexacraft.world.coord.integer.{BlockRelWorld, ChunkRelWorld, Offset}
import org.joml.{Matrix4f, Vector4f}
import org.lwjgl.BufferUtils

import java.nio.ByteBuffer
import java.util
import scala.collection.mutable.ArrayBuffer

object ChunkRenderDataFactory:
  def makeChunkRenderData(
      chunkCoords: ChunkRelWorld,
      blocks: Array[LocalBlockState],
      world: BlocksInWorld
  )(using CylinderSize)(using Blocks: Blocks): ChunkRenderData =
    val chunkCache = new ChunkCache(world)

    val sidesToRender = Array.tabulate[util.BitSet](8)(_ => new util.BitSet(16 * 16 * 16))
    val sideBrightness = Array.ofDim[Float](8, 16 * 16 * 16)
    val sidesCount = Array.ofDim[Int](8)

    for s <- 0 until 8 do
      val shouldRender = sidesToRender(s)
      val shouldRenderTop = sidesToRender(0)
      val brightness = sideBrightness(s)
      val otherSide = oppositeSide(s)

      var i1 = 0
      val i1Lim = blocks.length
      while i1 < i1Lim do
        val lbs = blocks(i1)
        val c = lbs.coords
        val b = lbs.block

        val c2w = c.globalNeighbor(s, chunkCoords)
        val c2 = c2w.getBlockRelChunk
        val crw = c2w.getChunkRelWorld
        val neigh = chunkCache.getChunk(crw)

        if neigh != null then
          val bs = neigh.getBlock(c2)

          if bs.blockType.isTransparent(bs.metadata, otherSide) then
            brightness(c.value) = neigh.lighting.getBrightness(c2)
            shouldRender.set(c.value)
            sidesCount(s) += 1

            // render the top side
            if s > 1 && b.blockType.isTransparent(b.metadata, s) then
              shouldRenderTop.set(c.value)
              sidesCount(0) += 1

        i1 += 1

    val buffers = for (side <- 0 until 8) yield
      val shouldRender = sidesToRender(side)
      val brightness = sideBrightness(side)
      val buf = BufferUtils.createByteBuffer(sidesCount(side) * ChunkRenderData.blockSideStride(side))

      val brightnessFn = (coords: BlockRelWorld) => {
        val cc = coords.getChunkRelWorld
        val bc = coords.getBlockRelChunk

        Option(chunkCache.getChunk(cc)) match
          case Some(ch) => ch.lighting.getBrightness(bc)
          case None     => 0
      }

      populateBuffer(chunkCoords, blocks, side, shouldRender, brightnessFn, buf)
      buf.flip()
      buf

    ChunkRenderData(buffers)

  private def populateBuffer(
      chunkCoords: ChunkRelWorld,
      blocks: Array[LocalBlockState],
      side: Int,
      shouldRender: java.util.BitSet,
      brightness: BlockRelWorld => Float,
      buf: ByteBuffer
  )(using Blocks: Blocks, cylSize: CylinderSize): Unit =
    val verticesPerInstance = if (side < 2) 7 else 4

    var i1 = 0
    val i1Lim = blocks.length
    while i1 < i1Lim do
      val lbs = blocks(i1)
      val bCoords = lbs.coords
      val block = lbs.block

      if shouldRender.get(bCoords.value) then
        val coords = BlockRelWorld.fromChunk(bCoords, chunkCoords)
        buf.putInt(coords.x)
        buf.putInt(coords.y)
        buf.putInt(coords.z)

        val blockType = block.blockType
        buf.putInt(Blocks.textures(blockType.name)(side))
        buf.putFloat(blockType.blockHeight(block.metadata))

        var i2 = 0
        while i2 < verticesPerInstance do
          val offsets =
            side match
              case 0 | 1 =>
                i2 match
                  case 0 => Seq(Offset(1, 0, 0), Offset(1, 0, -1))
                  case 1 => Seq(Offset(0, 0, 1), Offset(1, 0, 0))
                  case 2 => Seq(Offset(-1, 0, 1), Offset(0, 0, 1))
                  case 3 => Seq(Offset(-1, 0, 0), Offset(-1, 0, 1))
                  case 4 => Seq(Offset(0, 0, -1), Offset(-1, 0, 0))
                  case 5 => Seq(Offset(1, 0, -1), Offset(0, 0, -1))
                  case _ => Seq(Offset(0, 0, 0), Offset(0, 0, 0))
              case _ =>
                i2 match
                  case 0 => Seq(Offset(0, 1, 0))
                  case 1 => Seq(Offset(0, 1, 0))
                  case 2 => Seq(Offset(0, -1, 0))
                  case _ => Seq(Offset(0, -1, 0))

          val globalBCoords = bCoords.globalNeighbor(side, chunkCoords)
          val brs = (offsets :+ Offset(0, 0, 0)).map(off => brightness(globalBCoords.offset(off))).filter(_ != 0)
          buf.putFloat(if brs.isEmpty then brightness(globalBCoords) else brs.sum / brs.size)
          i2 += 1

      i1 += 1

  private def oppositeSide(s: Int): Int =
    if s < 2 then 1 - s else (s - 2 + 3) % 3 + 2

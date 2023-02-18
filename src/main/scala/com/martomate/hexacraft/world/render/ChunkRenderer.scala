package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.{BlocksInWorld, ChunkCache}
import com.martomate.hexacraft.world.block.Blocks
import com.martomate.hexacraft.world.chunk.{Chunk, EntitiesInChunk}
import com.martomate.hexacraft.world.chunk.storage.LocalBlockState
import com.martomate.hexacraft.world.coord.CoordUtils
import com.martomate.hexacraft.world.coord.fp.CylCoords
import com.martomate.hexacraft.world.coord.integer.{BlockRelWorld, ChunkRelWorld}

import java.nio.ByteBuffer
import java.util
import org.joml.{Matrix4f, Vector4f}
import org.lwjgl.BufferUtils
import scala.collection.mutable.ArrayBuffer

object ChunkRenderer:
  def getChunkRenderData(
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

      populateBuffer(chunkCoords, blocks, side, shouldRender, brightness, buf)
      buf.flip()
      buf

    ChunkRenderData(buffers)

  private def populateBuffer(
      chunkCoords: ChunkRelWorld,
      blocks: Array[LocalBlockState],
      side: Int,
      shouldRender: java.util.BitSet,
      brightness: Array[Float],
      buf: ByteBuffer
  )(using Blocks: Blocks): Unit =
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
          // TODO: change in the future to make lighting smoother
          buf.putFloat(brightness(bCoords.value))
          i2 += 1

      i1 += 1

  def getEntityRenderData(entities: EntitiesInChunk, side: Int, world: BlocksInWorld)(using
      CylinderSize
  ): Iterable[EntityDataForShader] =
    val chunkCache = new ChunkCache(world)

    val tr = new Matrix4f

    for ent <- entities.allEntities yield
      val baseT = ent.transform
      val model = ent.model

      val parts = for part <- model.parts yield
        baseT.mul(part.transform, tr)

        val coords4 = tr.transform(new Vector4f(0, 0.5f, 0, 1))
        val blockCoords = CylCoords(coords4.x, coords4.y, coords4.z).toBlockCoords
        val coords = CoordUtils.getEnclosingBlock(blockCoords)._1
        val cCoords = coords.getChunkRelWorld

        val partChunk = chunkCache.getChunk(cCoords)

        val brightness: Float =
          if partChunk != null
          then partChunk.lighting.getBrightness(coords.getBlockRelChunk)
          else 0

        EntityPartDataForShader(
          modelMatrix = new Matrix4f(tr),
          texOffset = part.textureOffset(side),
          texSize = part.textureSize(side),
          blockTex = part.texture(side),
          brightness
        )

      EntityDataForShader(model, parts)

  private def oppositeSide(s: Int): Int =
    if s < 2 then 1 - s else (s - 2 + 3) % 3 + 2

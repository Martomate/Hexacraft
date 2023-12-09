package hexacraft.world.render

import hexacraft.world.{BlocksInWorld, ChunkCache, CylinderSize}
import hexacraft.world.block.BlockSpecRegistry
import hexacraft.world.chunk.Chunk
import hexacraft.world.chunk.storage.LocalBlockState
import hexacraft.world.coord.integer.{BlockRelWorld, ChunkRelWorld, Offset}

import org.lwjgl.BufferUtils

import java.nio.ByteBuffer
import java.util
import scala.collection.mutable.ArrayBuffer

object ChunkRenderDataFactory:
  def makeChunkRenderData(
      chunkCoords: ChunkRelWorld,
      blocks: Array[LocalBlockState],
      world: BlocksInWorld,
      transmissiveBlocks: Boolean,
      blockSpecs: BlockSpecRegistry
  )(using CylinderSize): ChunkRenderData =
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
        val state = blocks(i1)
        val c = state.coords
        val b = state.block

        if b.blockType.canBeRendered then
          if !transmissiveBlocks && !b.blockType.isTransmissive then
            val c2w = c.globalNeighbor(s, chunkCoords)
            val c2 = c2w.getBlockRelChunk
            val crw = c2w.getChunkRelWorld
            val neigh = chunkCache.getChunk(crw)

            if neigh != null then
              val bs = neigh.getBlock(c2)

              if !bs.blockType.isCovering(bs.metadata, otherSide) || bs.blockType.isTransmissive then
                brightness(c.value) = neigh.lighting.getBrightness(c2)
                shouldRender.set(c.value)
                sidesCount(s) += 1

                // render the top side
                if s > 1 && !b.blockType.isCovering(b.metadata, s) then
                  shouldRenderTop.set(c.value)
                  sidesCount(0) += 1
          else if transmissiveBlocks && b.blockType.isTransmissive then
            val c2w = c.globalNeighbor(s, chunkCoords)
            val c2 = c2w.getBlockRelChunk
            val crw = c2w.getChunkRelWorld
            val neigh = chunkCache.getChunk(crw)

            if neigh != null then
              val bs = neigh.getBlock(c2)

              if b != bs then
                brightness(c.value) = neigh.lighting.getBrightness(c2)
                shouldRender.set(c.value)
                sidesCount(s) += 1

        i1 += 1

    val blocksBuffers = for (side <- 0 until 8) yield
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

      populateBuffer(chunkCoords, blocks, side, shouldRender, brightnessFn, buf, blockSpecs)
      buf.flip()
      buf

    ChunkRenderData(blocksBuffers)

  private def populateBuffer(
      chunkCoords: ChunkRelWorld,
      blocks: Array[LocalBlockState],
      side: Int,
      shouldRender: java.util.BitSet,
      brightness: BlockRelWorld => Float,
      buf: ByteBuffer,
      blockSpecs: BlockSpecRegistry
  )(using CylinderSize): Unit =
    val verticesPerInstance = if (side < 2) 7 else 4

    var i1 = 0
    val i1Lim = blocks.length
    while i1 < i1Lim do
      val lbs = blocks(i1)
      val localCoords = lbs.coords
      val block = lbs.block

      if shouldRender.get(localCoords.value) then
        val worldCoords = BlockRelWorld.fromChunk(localCoords, chunkCoords)
        buf.putInt(worldCoords.x)
        buf.putInt(worldCoords.y)
        buf.putInt(worldCoords.z)

        val blockType = block.blockType
        buf.putInt(blockSpecs.textureIndex(blockType.name, side))
        buf.putFloat(blockType.blockHeight(block.metadata))

        var i2 = 0
        while i2 < verticesPerInstance do
          // all the blocks adjacent to this vertex (on the given side)
          val b = new ArrayBuffer[Offset](3)
          b += Offset(0, 0, 0)

          side match
            case 0 | 1 =>
              i2 match
                case 0 => b += Offset(1, 0, 0); b += Offset(1, 0, -1)
                case 1 => b += Offset(0, 0, 1); b += Offset(1, 0, 0)
                case 2 => b += Offset(-1, 0, 1); b += Offset(0, 0, 1)
                case 3 => b += Offset(-1, 0, 0); b += Offset(-1, 0, 1)
                case 4 => b += Offset(0, 0, -1); b += Offset(-1, 0, 0)
                case 5 => b += Offset(1, 0, -1); b += Offset(0, 0, -1)
                case _ => b += Offset(0, 0, 0); b += Offset(0, 0, 0) // extra point at the center
            case _ =>
              i2 match
                case 0 => b += Offset(0, 1, 0)
                case 1 => b += Offset(0, 1, 0)
                case 2 => b += Offset(0, -1, 0)
                case _ => b += Offset(0, -1, 0)

          val globalBCoords = localCoords.globalNeighbor(side, chunkCoords)

          var brSum = 0f
          var brCount = 0
          var bIdx = 0
          val bLen = b.length
          while bIdx < bLen do
            val br = brightness(globalBCoords.offset(b(bIdx)))
            if br != 0 then
              brSum += br
              brCount += 1
            bIdx += 1

          buf.putFloat(if brCount == 0 then brightness(globalBCoords) else brSum / brCount)
          i2 += 1

      i1 += 1

  private def oppositeSide(s: Int): Int =
    if s < 2 then 1 - s else (s - 2 + 3) % 3 + 2

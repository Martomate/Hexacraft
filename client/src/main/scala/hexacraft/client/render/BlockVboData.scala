package hexacraft.client.render

import hexacraft.shaders.BlockShader
import hexacraft.shaders.BlockShader.BlockVertexData
import hexacraft.world.{BlocksInWorld, ChunkCache, CylinderSize}
import hexacraft.world.block.{Block, BlockState}
import hexacraft.world.chunk.LocalBlockState
import hexacraft.world.coord.{BlockRelWorld, ChunkRelWorld, Offset}

import org.joml.{Vector2f, Vector3f, Vector3i}
import org.lwjgl.BufferUtils

import java.nio.ByteBuffer
import java.util
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object BlockVboData {
  private val topBottomVertexIndices = Seq(1, 6, 0, 6, 1, 2, 2, 3, 6, 4, 6, 3, 6, 4, 5, 5, 0, 6)
  private val sideVertexIndices = Seq(0, 1, 3, 2, 0, 3)
  private val cornerOffsets = Seq((2, 0), (1, 1), (-1, 1), (-2, 0), (-1, -1), (1, -1))
  private val normals =
    for s <- 0 until 8 yield {
      if s < 2 then {
        new Vector3f(0, 1f - 2f * s, 0)
      } else {
        val nv = ((s - 1) % 6 - 0.5) * Math.PI / 3
        val nx = Math.cos(nv).toFloat
        val nz = Math.sin(nv).toFloat
        new Vector3f(nx, 0, nz)
      }
    }

  def fromChunk(
      chunkCoords: ChunkRelWorld,
      blocks: Array[LocalBlockState],
      world: BlocksInWorld,
      transmissiveBlocks: Boolean,
      blockTextureIndices: Map[String, IndexedSeq[Int]]
  )(using CylinderSize): IndexedSeq[ByteBuffer] = {
    val chunkCache = new ChunkCache(world)

    val sidesToRender = Array.tabulate[util.BitSet](8)(_ => new util.BitSet(16 * 16 * 16))
    val sideBrightness = Array.ofDim[Float](8, 16 * 16 * 16)
    val sidesCount = Array.ofDim[Int](8)

    for s <- 0 until 8 do {
      val shouldRender = sidesToRender(s)
      val shouldRenderTop = sidesToRender(0)
      val brightness = sideBrightness(s)
      val otherSide = oppositeSide(s)

      for LocalBlockState(c, b) <- blocks do {
        if b.blockType.canBeRendered then {
          if !transmissiveBlocks && !b.blockType.isTransmissive then {
            val c2w = c.globalNeighbor(s, chunkCoords)
            val c2 = c2w.getBlockRelChunk
            val crw = c2w.getChunkRelWorld
            val neigh = chunkCache.getChunk(crw)

            if neigh != null then {
              val bs = neigh.getBlock(c2)

              if !bs.blockType.isCovering(bs.metadata, otherSide) || bs.blockType.isTransmissive then {
                brightness(c.value) = neigh.lighting.getBrightness(c2)
                shouldRender.set(c.value)
                sidesCount(s) += 1

                // render the top side
                if s > 1 && !b.blockType.isCovering(b.metadata, s) then {
                  shouldRenderTop.set(c.value)
                  sidesCount(0) += 1
                }
              }
            }
          } else if transmissiveBlocks && b.blockType.isTransmissive then {
            val c2w = c.globalNeighbor(s, chunkCoords)
            val c2 = c2w.getBlockRelChunk
            val crw = c2w.getChunkRelWorld
            val neigh = chunkCache.getChunk(crw)

            if neigh != null then {
              val bs = neigh.getBlock(c2)

              val shouldRenderSide = if bs.blockType == Block.Water then {
                b != bs && s < 2 && b.blockType != Block.Water
              } else {
                b != bs && (s == 0 || !bs.blockType.isSolid)
              }

              if shouldRenderSide then {
                brightness(c.value) = neigh.lighting.getBrightness(c2)
                shouldRender.set(c.value)
                sidesCount(s) += 1
              }
            }
          }
        }
      }
    }

    val blocksBuffers = for side <- 0 until 8 yield {
      val shouldRender = sidesToRender(side)
      val brightness = sideBrightness(side)

      val blockAtFn = (coords: BlockRelWorld) => {
        val ch = chunkCache.getChunk(coords.getChunkRelWorld)
        if ch != null then {
          ch.getBlock(coords.getBlockRelChunk)
        } else {
          BlockState.Air
        }
      }

      val brightnessFn = (coords: BlockRelWorld) => {
        val ch = chunkCache.getChunk(coords.getChunkRelWorld)
        if ch != null then {
          ch.lighting.getBrightness(coords.getBlockRelChunk)
        } else {
          0
        }
      }

      val cornersPerBlock = if side < 2 then 7 else 4

      val vertices = new mutable.ArrayBuffer[BlockVertexData](blocks.length)

      for LocalBlockState(localCoords, block) <- blocks if shouldRender.get(localCoords.value) do {
        val normal = normals(side)
        val worldCoords = BlockRelWorld.fromChunk(localCoords, chunkCoords)
        val neighborWorldCoords = localCoords.globalNeighbor(side, chunkCoords)
        val blockTex = blockTextureIndices(block.blockType.name)(side)

        val cornerHeights = Array.ofDim[Float](7)
        val cornerBrightnesses = Array.ofDim[Float](7)

        for cornerIdx <- 0 until cornersPerBlock do {
          cornerHeights(cornerIdx) = calculateCornerHeight(block, worldCoords, blockAtFn, cornerIdx, side)
          cornerBrightnesses(cornerIdx) = calculateCornerBrightness(neighborWorldCoords, brightnessFn, cornerIdx, side)
        }

        if side < 2 then {
          for i <- 0 until 6 * 3 do {
            val faceIndex = i / 3
            val vertexId = i % 3
            val a = topBottomVertexIndices(i)

            val (x, z) =
              if a == 6 then {
                (0, 0)
              } else {
                val (x, z) = cornerOffsets(a)
                if side == 0 then (-x, z) else (x, z)
              }

            val tex = vertexId match {
              case 0 => new Vector2f(0.5f, 1)
              case 1 => new Vector2f(0, 0)
              case 2 => new Vector2f(1, 0)
            }

            val height = cornerHeights(a)
            val brightness = cornerBrightnesses(a)

            val pos = Vector3i(
              worldCoords.x * 3 + x,
              worldCoords.y * 32 * 6 + ((1 - side) * 32 * 6 * height).toInt,
              worldCoords.z * 2 + worldCoords.x + z
            )
            val texIndex = (blockTex & 0xfff) + ((blockTex >> (4 * (5 - faceIndex)) & 0xffff) >> 12 & 15)

            vertices += BlockVertexData(pos, texIndex, normal, brightness, tex)
          }
        } else {
          for i <- 0 until 2 * 3 do {
            val a = sideVertexIndices(i)
            val v = (side - 2 + a % 2) % 6
            val (x, z) = cornerOffsets(v)

            val height = cornerHeights(a)
            val brightness = cornerBrightnesses(a)

            val pos = Vector3i(
              worldCoords.x * 3 + x,
              worldCoords.y * 32 * 6 + ((1 - a / 2) * 32 * 6 * height).toInt,
              worldCoords.z * 2 + worldCoords.x + z
            )
            val texIndex = blockTex & 0xfff
            val tex = Vector2f((1 - a % 2).toFloat, (a / 2).toFloat)

            vertices += BlockVertexData(pos, texIndex, normal, brightness, tex)
          }
        }
      }

      val bytesPerBlock = BlockShader.bytesPerVertex(side) * BlockShader.verticesPerBlock(side)
      val buf = BufferUtils.createByteBuffer(sidesCount(side) * bytesPerBlock)
      for v <- vertices do {
        v.fill(buf)
      }

      buf.flip()
      buf
    }

    blocksBuffers
  }

  private def calculateCornerHeight(
      block: BlockState,
      blockCoords: BlockRelWorld,
      blockAt: BlockRelWorld => BlockState,
      cornerIdx: Int,
      side: Int
  )(using CylinderSize): Float = {
    if block.blockType != Block.Water then {
      return block.blockType.blockHeight(block.metadata)
    }

    val corner = side match {
      case 0 =>
        cornerIdx match {
          case 3 => 0
          case 2 => 1
          case 1 => 2
          case 0 => 3
          case 5 => 4
          case 4 => 5
          case 6 => 6
        }
      case 1 => cornerIdx
      case s => s - 2
    }

    val b = new ArrayBuffer[Offset](3)
    b += Offset(0, 0, 0)

    corner match {
      case 0 => b += Offset(1, 0, 0); b += Offset(1, 0, -1)
      case 1 => b += Offset(0, 0, 1); b += Offset(1, 0, 0)
      case 2 => b += Offset(-1, 0, 1); b += Offset(0, 0, 1)
      case 3 => b += Offset(-1, 0, 0); b += Offset(-1, 0, 1)
      case 4 => b += Offset(0, 0, -1); b += Offset(-1, 0, 0)
      case 5 => b += Offset(1, 0, -1); b += Offset(0, 0, -1)
      case 6 => b += Offset(0, 0, 0); b += Offset(0, 0, 0) // extra point at the center
    }

    var hSum = 0f
    var hCount = 0
    var shouldBeMax = false

    var bIdx1 = 0
    val bLen1 = b.length
    while bIdx1 < bLen1 do {
      val above = blockAt(blockCoords.offset(b(bIdx1)).offset(0, 1, 0))
      if above.blockType == Block.Water then {
        shouldBeMax = true
      }

      val bs = blockAt(blockCoords.offset(b(bIdx1)))
      if bs.blockType == Block.Water then {
        hSum += bs.blockType.blockHeight(bs.metadata)
        hCount += 1
      }
      bIdx1 += 1
    }

    if shouldBeMax then 1.0f else hSum / hCount
  }

  private def calculateCornerBrightness(
      neighborBlockCoords: BlockRelWorld,
      brightness: BlockRelWorld => Float,
      cornerIdx: Int,
      side: Int
  )(using CylinderSize) = {
    // all the blocks adjacent to this vertex (on the given side)
    val b = new ArrayBuffer[Offset](3)
    b += Offset(0, 0, 0)

    side match {
      case 0 =>
        cornerIdx match {
          case 3 => b += Offset(1, 0, 0); b += Offset(1, 0, -1)
          case 2 => b += Offset(0, 0, 1); b += Offset(1, 0, 0)
          case 1 => b += Offset(-1, 0, 1); b += Offset(0, 0, 1)
          case 0 => b += Offset(-1, 0, 0); b += Offset(-1, 0, 1)
          case 5 => b += Offset(0, 0, -1); b += Offset(-1, 0, 0)
          case 4 => b += Offset(1, 0, -1); b += Offset(0, 0, -1)
          case 6 => b += Offset(0, 0, 0); b += Offset(0, 0, 0) // extra point at the center
        }
      case 1 =>
        cornerIdx match {
          case 0 => b += Offset(1, 0, 0); b += Offset(1, 0, -1)
          case 1 => b += Offset(0, 0, 1); b += Offset(1, 0, 0)
          case 2 => b += Offset(-1, 0, 1); b += Offset(0, 0, 1)
          case 3 => b += Offset(-1, 0, 0); b += Offset(-1, 0, 1)
          case 4 => b += Offset(0, 0, -1); b += Offset(-1, 0, 0)
          case 5 => b += Offset(1, 0, -1); b += Offset(0, 0, -1)
          case 6 => b += Offset(0, 0, 0); b += Offset(0, 0, 0) // extra point at the center
        }
      case _ =>
        cornerIdx match {
          case 0 => b += Offset(0, 1, 0)
          case 1 => b += Offset(0, 1, 0)
          case 2 => b += Offset(0, -1, 0)
          case 3 => b += Offset(0, -1, 0)
        }
    }

    var brSum = 0f
    var brCount = 0
    var bIdx = 0
    val bLen = b.length
    while bIdx < bLen do {
      val br = brightness(neighborBlockCoords.offset(b(bIdx)))
      if br != 0 then {
        brSum += br
        brCount += 1
      }
      bIdx += 1
    }
    if brCount == 0 then brightness(neighborBlockCoords) else brSum / brCount
  }

  private def oppositeSide(s: Int): Int = {
    if s < 2 then {
      1 - s
    } else {
      (s - 2 + 3) % 3 + 2
    }
  }
}

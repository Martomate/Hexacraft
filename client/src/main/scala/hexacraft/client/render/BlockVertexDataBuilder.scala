package hexacraft.client.render

import hexacraft.shaders.BlockShader.BlockVertexData
import hexacraft.util.Loop
import hexacraft.world.{BlocksInWorld, ChunkCache, CylinderSize}
import hexacraft.world.block.{Block, BlockState}
import hexacraft.world.chunk.LocalBlockState
import hexacraft.world.coord.{BlockRelWorld, ChunkRelWorld, Offset}

import org.joml.{Vector2f, Vector3f, Vector3i}

import java.util
import scala.collection.mutable.ArrayBuffer

object BlockVertexDataBuilder {
  def fromChunk(
      chunkCoords: ChunkRelWorld,
      world: BlocksInWorld,
      transmissiveBlocks: Boolean,
      blockTextureIndices: Map[String, IndexedSeq[Int]]
  )(using CylinderSize): BlockVertexDataBuilder = {
    val chunkCache = new ChunkCache(world)
    val blocks = chunkCache.getChunk(chunkCoords).blocks

    val sidesToRender = Array.tabulate[util.BitSet](8)(_ => new util.BitSet(16 * 16 * 16))

    Loop.rangeUntil(0, 8) { s =>
      val shouldRender = sidesToRender(s)
      val shouldRenderTop = sidesToRender(0)
      val otherSide = oppositeSide(s)

      Loop.array(blocks) { case LocalBlockState(c, b) =>
        if b.blockType.canBeRendered then {
          if !transmissiveBlocks && !b.blockType.isTransmissive then {
            val c2w = c.globalNeighbor(s, chunkCoords)
            val c2 = c2w.getBlockRelChunk
            val crw = c2w.getChunkRelWorld
            val neigh = chunkCache.getChunk(crw)

            if neigh != null then {
              val bs = neigh.getBlock(c2)

              if !bs.blockType.isCovering(bs.metadata, otherSide) || bs.blockType.isTransmissive then {
                shouldRender.set(c.value)

                // render the top side
                if s > 1 && !b.blockType.isCovering(b.metadata, s) then {
                  shouldRenderTop.set(c.value)
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

              val here = b.blockType
              val there = bs.blockType

              val shouldRenderSide =
                if here == there then false
                else if there.isTransmissive then true
                else if s == 0 && here.blockHeight(b.metadata) < 1.0 then true
                else false

              // TODO: sort translucent faces and the rendering glitches might be fixed

              if shouldRenderSide then {
                shouldRender.set(c.value)
              }
            }
          }
        }
      }
    }

    new BlockVertexDataBuilder(chunkCache, blockTextureIndices, blocks, chunkCoords, sidesToRender)
  }

  private def oppositeSide(s: Int): Int = {
    if s < 2 then {
      1 - s
    } else {
      (s - 2 + 3) % 3 + 2
    }
  }
}

class BlockVertexDataBuilder(
    chunkCache: ChunkCache,
    blockTextureIndices: Map[String, IndexedSeq[Int]],
    blocks: Array[LocalBlockState],
    chunkCoords: ChunkRelWorld,
    sidesToRender: Array[util.BitSet]
)(using CylinderSize) {
  private val topBottomVertexIndices = Array(1, 6, 0, 6, 1, 2, 2, 3, 6, 4, 6, 3, 6, 4, 5, 5, 0, 6)
  private val sideVertexIndices = Array(0, 1, 3, 2, 0, 3)
  private val cornerOffsets = Array((2, 0), (1, 1), (-1, 1), (-2, 0), (-1, -1), (1, -1))
  private val normals =
    Array.tabulate(8) { s =>
      if s < 2 then {
        new Vector3f(0, 1f - 2f * s, 0)
      } else {
        val nv = ((s - 1) % 6 - 0.5) * Math.PI / 3
        val nx = Math.cos(nv).toFloat
        val nz = Math.sin(nv).toFloat
        new Vector3f(nx, 0, nz)
      }
    }

  private def blockAt(coords: BlockRelWorld): BlockState = {
    val ch = chunkCache.getChunk(coords.getChunkRelWorld)
    if ch != null then {
      ch.getBlock(coords.getBlockRelChunk)
    } else {
      BlockState.Air
    }
  }

  private def brightnessAt(coords: BlockRelWorld): Float = {
    val ch = chunkCache.getChunk(coords.getChunkRelWorld)
    if ch != null then {
      ch.getBrightness(coords.getBlockRelChunk)
    } else {
      0
    }
  }

  def calculateVertices(side: Int): ArrayBuffer[BlockVertexData] = {
    val shouldRender = sidesToRender(side)

    val cornersPerBlock = if side < 2 then 7 else 4

    val vertices = new ArrayBuffer[BlockVertexData](blocks.length)

    Loop.array(blocks) { case LocalBlockState(localCoords, block) =>
      if shouldRender.get(localCoords.value) then {
        val normal = normals(side)
        val worldCoords = BlockRelWorld.fromChunk(localCoords, chunkCoords)
        val neighborWorldCoords = localCoords.globalNeighbor(side, chunkCoords)
        val blockTex = blockTextureIndices(block.blockType.name)(side)

        val cornerHeights = Array.ofDim[Float](7)
        val cornerBrightnesses = Array.ofDim[Float](7)

        Loop.rangeUntil(0, cornersPerBlock) { cornerIdx =>
          cornerHeights(cornerIdx) = calculateCornerHeight(block, worldCoords, cornerIdx, side)
          cornerBrightnesses(cornerIdx) = calculateCornerBrightness(neighborWorldCoords, cornerIdx, side)
        }

        if side < 2 then {
          Loop.rangeUntil(0, 6 * 3) { i =>
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
          Loop.rangeUntil(0, 2 * 3) { i =>
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
    }
    vertices
  }

  private def calculateCornerHeight(block: BlockState, blockCoords: BlockRelWorld, cornerIdx: Int, side: Int): Float = {
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

    Loop.array(b) { off =>
      val coords = blockCoords.offset(off)

      val above = blockAt(coords.offset(0, 1, 0))
      if above.blockType == Block.Water then {
        shouldBeMax = true
      }

      val bs = blockAt(coords)
      if bs.blockType == Block.Water then {
        hSum += bs.blockType.blockHeight(bs.metadata)
        hCount += 1
      }
    }

    if shouldBeMax then 1.0f else hSum / hCount
  }

  private def calculateCornerBrightness(blockAheadCoords: BlockRelWorld, cornerIdx: Int, side: Int) = {
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

    // TODO: calculate ambient occlusion based on the neighbors, and make sure it's independent of `side`

    var brSum = 0f
    var brCount = 0

    Loop.array(b) { off =>
      val br = brightnessAt(blockAheadCoords.offset(off))
      if br != 0 then {
        brSum += br
        brCount += 1
      }
    }

    if brCount == 0 then {
      brightnessAt(blockAheadCoords)
    } else {
      val ambientOcclusionFactor = (brCount - 1).toFloat / (b.length - 1) * 0.2f + 0.8f // TODO: temporary
      brSum / brCount * ambientOcclusionFactor
    }
  }
}

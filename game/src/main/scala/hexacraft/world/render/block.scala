package hexacraft.world.render

import hexacraft.infra.gpu.OpenGL
import hexacraft.renderer.{Shader, ShaderConfig, VAO}
import hexacraft.world.{BlocksInWorld, ChunkCache, CylinderSize}
import hexacraft.world.block.{Block, BlockState}
import hexacraft.world.chunk.LocalBlockState
import hexacraft.world.coord.{BlockRelWorld, ChunkRelWorld, Offset}

import org.joml.{Matrix4f, Vector2f, Vector3d, Vector3f}
import org.lwjgl.BufferUtils

import java.nio.ByteBuffer
import java.util
import scala.collection.mutable.ArrayBuffer

class BlockShader(isSide: Boolean) {
  private val config = ShaderConfig("block")
    .withInputs(
      "position",
      "texCoords",
      "faceIndex",
      "normal",
      "blockTex",
      "vertexData"
    )
    .withDefines("isSide" -> (if isSide then "1" else "0"))

  private val shader = Shader.from(config)

  def setTotalSize(totalSize: Int): Unit = {
    shader.setUniform1i("totalSize", totalSize)
  }

  def setSunPosition(sun: Vector3f): Unit = {
    shader.setUniform3f("sun", sun.x, sun.y, sun.z)
  }

  def setCameraPosition(cam: Vector3d): Unit = {
    shader.setUniform3f("cam", cam.x.toFloat, cam.y.toFloat, cam.z.toFloat)
  }

  def setProjectionMatrix(matrix: Matrix4f): Unit = {
    shader.setUniformMat4("projMatrix", matrix)
  }

  def setViewMatrix(matrix: Matrix4f): Unit = {
    shader.setUniformMat4("viewMatrix", matrix)
  }

  def setSide(side: Int): Unit = {
    shader.setUniform1i("side", side)
  }

  def enable(): Unit = {
    shader.activate()
  }

  def free(): Unit = {
    shader.free()
  }
}

object BlockVao {
  private[render] def verticesPerBlock(side: Int): Int = {
    if side < 2 then {
      3 * 6
    } else {
      3 * 2
    }
  }

  def bytesPerVertex(side: Int): Int = (6 + 4 + 2) * 4

  def forSide(side: Int)(maxVertices: Int): VAO = {
    val verticesPerInstance = BlockVao.verticesPerBlock(side)

    VAO
      .builder()
      .addVertexVbo(maxVertices, OpenGL.VboUsage.DynamicDraw)(
        _.ints(0, 3)
          .floats(1, 2)
          .ints(2, 1)
          .floats(3, 3)
          .ints(4, 1)
          .floats(5, 2)
      )
      .finish(maxVertices)
  }
}

object AdvancedBlockRenderer {
  private[render] val topBottomVertexIndices = Seq(1, 6, 0, 6, 1, 2, 2, 3, 6, 4, 6, 3, 6, 4, 5, 5, 0, 6)
  private[render] val sideVertexIndices = Seq(0, 1, 3, 2, 0, 3)
}

object BlockVboData {
  private def blockSideStride(side: Int): Int = BlockVao.bytesPerVertex(side)

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

      var i1 = 0
      val i1Lim = blocks.length
      while i1 < i1Lim do {
        val state = blocks(i1)
        val c = state.coords
        val b = state.block

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

              if b != bs && (s == 0 || !bs.blockType.isSolid) && (s < 2 || bs.blockType != Block.Water) then {
                brightness(c.value) = neigh.lighting.getBrightness(c2)
                shouldRender.set(c.value)
                sidesCount(s) += 1
              }
            }
          }
        }

        i1 += 1
      }
    }

    val blocksBuffers = for side <- 0 until 8 yield {
      val shouldRender = sidesToRender(side)
      val brightness = sideBrightness(side)

      val blockAtFn = (coords: BlockRelWorld) => {
        val cc = coords.getChunkRelWorld
        val bc = coords.getBlockRelChunk

        Option(chunkCache.getChunk(cc)) match {
          case Some(ch) => ch.getBlock(bc)
          case None     => BlockState.Air
        }
      }

      val brightnessFn = (coords: BlockRelWorld) => {
        val cc = coords.getChunkRelWorld
        val bc = coords.getBlockRelChunk

        Option(chunkCache.getChunk(cc)) match {
          case Some(ch) => ch.lighting.getBrightness(bc)
          case None     => 0
        }
      }

      val bytesPerBlock = BlockVao.bytesPerVertex(side) * BlockVao.verticesPerBlock(side)
      val buf = BufferUtils.createByteBuffer(sidesCount(side) * bytesPerBlock)
      populateBuffer(chunkCoords, blocks, side, shouldRender, blockAtFn, brightnessFn, buf, blockTextureIndices)
      buf.flip()
      buf
    }

    blocksBuffers
  }

  private def populateBuffer(
      chunkCoords: ChunkRelWorld,
      blocks: Array[LocalBlockState],
      side: Int,
      shouldRender: java.util.BitSet,
      blockAt: BlockRelWorld => BlockState,
      brightness: BlockRelWorld => Float,
      buf: ByteBuffer,
      blockTextureIndices: Map[String, IndexedSeq[Int]]
  )(using CylinderSize): Unit = {
    val verticesPerInstance = if side < 2 then 7 else 4

    var i1 = 0
    val i1Lim = blocks.length
    while i1 < i1Lim do {
      val lbs = blocks(i1)
      val localCoords = lbs.coords
      val block = lbs.block

      if shouldRender.get(localCoords.value) then {
        val normal = normals(side)
        val worldCoords = BlockRelWorld.fromChunk(localCoords, chunkCoords)
        val neighborWorldCoords = localCoords.globalNeighbor(side, chunkCoords)
        val blockTex = blockTextureIndices(block.blockType.name)(side)

        val vertexData = Array.ofDim[Vector2f](7)
        var cornerIdx = 0
        while cornerIdx < verticesPerInstance do {
          vertexData(cornerIdx) = Vector2f(
            calculateCornerHeight(block, worldCoords, blockAt, cornerIdx, side),
            calculateCornerBrightness(neighborWorldCoords, brightness, cornerIdx, side)
          )
          cornerIdx += 1
        }

        if side < 2 then {
          for i <- 0 until 6 * 3 do {
            val faceIndex = i / 3
            val vertexId = i % 3
            val a = AdvancedBlockRenderer.topBottomVertexIndices(i)

            val (x, z) =
              if a == 6 then {
                (0, 0)
              } else {
                val (x, z) = a match {
                  case 0 => (2, 0)
                  case 1 => (1, 1)
                  case 2 => (-1, 1)
                  case 3 => (-2, 0)
                  case 4 => (-1, -1)
                  case 5 => (1, -1)
                }
                if side == 0 then (-x, z) else (x, z)
              }

            val tex = vertexId match {
              case 0 => new Vector2f(0.5f, 1)
              case 1 => new Vector2f(0, 0)
              case 2 => new Vector2f(1, 0)
            }

            buf.putInt(worldCoords.x * 3 + x)
            buf.putInt(worldCoords.y + 1 - side)
            buf.putInt(worldCoords.z * 2 + worldCoords.x + z)

            buf.putFloat(tex.x)
            buf.putFloat(tex.y)

            buf.putInt(faceIndex)

            buf.putFloat(normal.x)
            buf.putFloat(normal.y)
            buf.putFloat(normal.z)

            buf.putInt(blockTex)

            val data = vertexData(a)
            buf.putFloat(data.x)
            buf.putFloat(data.y)
          }
        } else {
          for i <- 0 until 2 * 3 do {
            val faceIndex = i / 3
            val vertexId = i % 3
            val a = AdvancedBlockRenderer.sideVertexIndices(i)

            val v = (side - 2 + a % 2) % 6
            val (x, z) = v match {
              case 0 => (2, 0)
              case 1 => (1, 1)
              case 2 => (-1, 1)
              case 3 => (-2, 0)
              case 4 => (-1, -1)
              case 5 => (1, -1)
            }

            buf.putInt(worldCoords.x * 3 + x)
            buf.putInt(worldCoords.y + 1 - a / 2)
            buf.putInt(worldCoords.z * 2 + worldCoords.x + z)

            buf.putFloat((1 - a % 2).toFloat)
            buf.putFloat((a / 2).toFloat)

            buf.putInt(0)

            buf.putFloat(normal.x)
            buf.putFloat(normal.y)
            buf.putFloat(normal.z)

            buf.putInt(blockTex)

            val data = vertexData(a)
            buf.putFloat(data.x)
            buf.putFloat(data.y)
          }
        }
      }

      i1 += 1
    }
  }

  private def calculateCornerHeight(
      block: BlockState,
      blockCoords: BlockRelWorld,
      blockAt: BlockRelWorld => BlockState,
      cornerIdx: Int,
      side: Int
  )(using CylinderSize) = {
    if block.blockType == Block.Water then {
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
      var bIdx1 = 0
      val bLen1 = b.length
      while bIdx1 < bLen1 do {
        val bs = blockAt(blockCoords.offset(b(bIdx1)))
        if bs.blockType == Block.Water then {
          hSum += bs.blockType.blockHeight(bs.metadata)
          hCount += 1
        }
        bIdx1 += 1
      }

      hSum / hCount
    } else {
      block.blockType.blockHeight(block.metadata)
    }
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

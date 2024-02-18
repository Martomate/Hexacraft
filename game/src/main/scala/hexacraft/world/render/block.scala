package hexacraft.world.render

import hexacraft.infra.gpu.OpenGL
import hexacraft.renderer.{Shader, ShaderConfig, VAO, VertexData}
import hexacraft.world.{BlocksInWorld, ChunkCache, CylinderSize}
import hexacraft.world.block.{Block, BlockState}
import hexacraft.world.chunk.LocalBlockState
import hexacraft.world.coord.{BlockRelWorld, ChunkRelWorld, Offset}

import org.joml.{Matrix4f, Vector2f, Vector3d, Vector3f, Vector3i}
import org.lwjgl.BufferUtils

import java.nio.ByteBuffer
import java.util
import scala.collection.mutable.ArrayBuffer

class BlockShader(isSide: Boolean) {
  private val config = ShaderConfig("block")
    .withInputs(
      "position",
      "texCoords",
      "vertexIndex",
      "faceIndex",
      "normal",
      "blockPos",
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
  private def verticesPerInstance(side: Int): Int = {
    if side < 2 then {
      3 * 6
    } else {
      3 * 2
    }
  }

  private def cornersPerInstance(side: Int): Int = {
    if side < 2 then {
      7
    } else {
      4
    }
  }

  def bytesPerInstance(side: Int): Int = (7 + 2 * BlockVao.cornersPerInstance(side)) * 4

  def forSide(side: Int)(maxInstances: Int): VAO = {
    val verticesPerInstance = BlockVao.verticesPerInstance(side)
    val cornersPerInstance = BlockVao.cornersPerInstance(side)

    VAO
      .builder()
      .addVertexVbo(verticesPerInstance, OpenGL.VboUsage.StaticDraw)(
        _.ints(0, 3)
          .floats(1, 2)
          .ints(2, 1)
          .ints(3, 1),
        _.fill(0, AdvancedBlockRenderer.setupBlockVBO(side))
      )
      .addInstanceVbo(maxInstances, OpenGL.VboUsage.DynamicDraw)(
        _.floats(4, 3)
          .ints(5, 3)
          .ints(6, 1)
          .floatsArray(7, 2)(cornersPerInstance)
      )
      .finish(verticesPerInstance, maxInstances)
  }
}

object AdvancedBlockRenderer {
  case class BlockVertexData(
      position: Vector3i,
      texCoords: Vector2f,
      vertexIndex: Int,
      faceIndex: Int
  ) extends VertexData {

    override def bytesPerVertex: Int = (3 + 2 + 1 + 1) * 4

    override def fill(buf: ByteBuffer): Unit = {
      buf.putInt(position.x)
      buf.putInt(position.y)
      buf.putInt(position.z)

      buf.putFloat(texCoords.x)
      buf.putFloat(texCoords.y)

      buf.putInt(vertexIndex)
      buf.putInt(faceIndex)
    }
  }

  private val topBottomVertexIndices = Seq(1, 6, 0, 6, 1, 2, 2, 3, 6, 4, 6, 3, 6, 4, 5, 5, 0, 6)
  private val sideVertexIndices = Seq(0, 1, 3, 2, 0, 3)

  def verticesPerInstance(side: Int): Int = if side < 2 then 3 * 6 else 3 * 2

  def setupBlockVBO(s: Int): Seq[BlockVertexData] = {
    if s < 2 then {
      setupBlockVboForTopOrBottom(s)
    } else {
      setupBlockVboForSide(s)
    }
  }

  private def setupBlockVboForTopOrBottom(s: Int): Seq[BlockVertexData] = {
    val ints = topBottomVertexIndices

    for i <- 0 until verticesPerInstance(s) yield {
      val cornerIdx = i
      val a = ints(cornerIdx)
      val faceIndex = i / 3

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
          if s == 0 then (-x, z) else (x, z)
        }

      val pos = new Vector3i(x, 1 - s, z)
      val tex = cornerIdx % 3 match {
        case 0 => new Vector2f(0.5f, 1)
        case 1 => new Vector2f(0, 0)
        case 2 => new Vector2f(1, 0)
      }

      BlockVertexData(pos, tex, a, faceIndex)
    }
  }

  private def setupBlockVboForSide(s: Int): Seq[BlockVertexData] = {
    val ints = sideVertexIndices

    for i <- 0 until verticesPerInstance(s) yield {
      val a = ints(i)
      val v = (s - 2 + a % 2) % 6
      val (x, z) = v match {
        case 0 => (2, 0)
        case 1 => (1, 1)
        case 2 => (-1, 1)
        case 3 => (-2, 0)
        case 4 => (-1, -1)
        case 5 => (1, -1)
      }

      val pos = new Vector3i(x, 1 - a / 2, z)
      val tex = new Vector2f((1 - a % 2).toFloat, (a / 2).toFloat)

      BlockVertexData(pos, tex, a, 0)
    }
  }
}

object BlockVboData {
  private def blockSideStride(side: Int): Int = {
    if side < 2 then {
      (7 + 7 * 2) * 4
    } else {
      (7 + 4 * 2) * 4
    }
  }

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
      val buf = BufferUtils.createByteBuffer(sidesCount(side) * blockSideStride(side))

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
        // TODO: replace instanced rendering with regular triangles, and put the data into the buffer here
        val normal = normals(side)
        val worldCoords = BlockRelWorld.fromChunk(localCoords, chunkCoords)
        val neighborWorldCoords = localCoords.globalNeighbor(side, chunkCoords)

        buf.putFloat(normal.x)
        buf.putFloat(normal.y)
        buf.putFloat(normal.z)

        buf.putInt(worldCoords.x * 3)
        buf.putInt(worldCoords.y)
        buf.putInt(worldCoords.z * 2 + worldCoords.x)

        buf.putInt(blockTextureIndices(block.blockType.name)(side))

        var cornerIdx = 0
        while cornerIdx < verticesPerInstance do {
          val cornerHeight = calculateCornerHeight(block, worldCoords, blockAt, cornerIdx, side)
          val cornerBrightness = calculateCornerBrightness(neighborWorldCoords, brightness, cornerIdx, side)

          buf.putFloat(cornerHeight)
          buf.putFloat(cornerBrightness)

          cornerIdx += 1
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

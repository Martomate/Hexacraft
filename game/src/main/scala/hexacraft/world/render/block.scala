package hexacraft.world.render

import hexacraft.infra.gpu.OpenGL
import hexacraft.renderer.{Shader, ShaderConfig, VAO}
import hexacraft.world.{BlocksInWorld, ChunkCache, CylinderSize}
import hexacraft.world.block.{Block, BlockState}
import hexacraft.world.chunk.LocalBlockState
import hexacraft.world.coord.{BlockRelWorld, ChunkRelWorld, Offset}

import org.joml.{Matrix4f, Vector3d, Vector3f}
import org.lwjgl.BufferUtils

import java.nio.ByteBuffer
import java.util
import scala.collection.mutable.ArrayBuffer

class BlockShader(isSide: Boolean) {
  private val config = ShaderConfig("block")
    .withInputs(
      "position",
      "texCoords",
      "normal",
      "vertexIndex",
      "faceIndex",
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

  def bytesPerInstance(side: Int): Int = (5 + BlockVao.cornersPerInstance(side)) * 4

  def forSide(side: Int)(maxInstances: Int): VAO = {
    val verticesPerInstance = BlockVao.verticesPerInstance(side)
    val cornersPerInstance = BlockVao.cornersPerInstance(side)

    VAO
      .builder()
      .addVertexVbo(verticesPerInstance, OpenGL.VboUsage.StaticDraw)(
        _.floats(0, 3)
          .floats(1, 2)
          .floats(2, 3)
          .ints(3, 1)
          .ints(4, 1),
        _.fill(0, BlockRenderer.setupBlockVBO(side))
      )
      .addInstanceVbo(maxInstances, OpenGL.VboUsage.DynamicDraw)(
        _.ints(5, 3)
          .ints(6, 1)
          .floatsArray(7, 2)(cornersPerInstance)
      )
      .finish(verticesPerInstance, maxInstances)
  }
}

object BlockVboData:
  private def blockSideStride(side: Int): Int = {
    if side < 2 then {
      (5 + 7 * 2) * 4
    } else {
      (5 + 4 * 2) * 4
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
        val worldCoords = BlockRelWorld.fromChunk(localCoords, chunkCoords)
        buf.putInt(worldCoords.x)
        buf.putInt(worldCoords.y)
        buf.putInt(worldCoords.z)

        val blockType = block.blockType
        buf.putInt(blockTextureIndices(blockType.name)(side))

        var i2 = 0
        while i2 < verticesPerInstance do {
          if blockType == Block.Water then {
            val corner = side match {
              case 0 =>
                i2 match {
                  case 3 => 0
                  case 2 => 1
                  case 1 => 2
                  case 0 => 3
                  case 5 => 4
                  case 4 => 5
                  case 6 => 6
                }
              case 1 => i2
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

            val globalBCoords = BlockRelWorld.fromChunk(localCoords, chunkCoords)

            var hSum = 0f
            var hCount = 0
            var bIdx1 = 0
            val bLen1 = b.length
            while bIdx1 < bLen1 do {
              val bs = blockAt(globalBCoords.offset(b(bIdx1)))
              if bs.blockType == Block.Water then {
                hSum += bs.blockType.blockHeight(bs.metadata)
                hCount += 1
              }
              bIdx1 += 1
            }

            buf.putFloat(hSum / hCount)
          } else {
            buf.putFloat(blockType.blockHeight(block.metadata))
          }

          // all the blocks adjacent to this vertex (on the given side)
          val b = new ArrayBuffer[Offset](3)
          b += Offset(0, 0, 0)

          side match {
            case 0 =>
              i2 match {
                case 3 => b += Offset(1, 0, 0); b += Offset(1, 0, -1)
                case 2 => b += Offset(0, 0, 1); b += Offset(1, 0, 0)
                case 1 => b += Offset(-1, 0, 1); b += Offset(0, 0, 1)
                case 0 => b += Offset(-1, 0, 0); b += Offset(-1, 0, 1)
                case 5 => b += Offset(0, 0, -1); b += Offset(-1, 0, 0)
                case 4 => b += Offset(1, 0, -1); b += Offset(0, 0, -1)
                case 6 => b += Offset(0, 0, 0); b += Offset(0, 0, 0) // extra point at the center
              }
            case 1 =>
              i2 match {
                case 0 => b += Offset(1, 0, 0); b += Offset(1, 0, -1)
                case 1 => b += Offset(0, 0, 1); b += Offset(1, 0, 0)
                case 2 => b += Offset(-1, 0, 1); b += Offset(0, 0, 1)
                case 3 => b += Offset(-1, 0, 0); b += Offset(-1, 0, 1)
                case 4 => b += Offset(0, 0, -1); b += Offset(-1, 0, 0)
                case 5 => b += Offset(1, 0, -1); b += Offset(0, 0, -1)
                case 6 => b += Offset(0, 0, 0); b += Offset(0, 0, 0) // extra point at the center
              }
            case _ =>
              i2 match {
                case 0 => b += Offset(0, 1, 0)
                case 1 => b += Offset(0, 1, 0)
                case 2 => b += Offset(0, -1, 0)
                case 3 => b += Offset(0, -1, 0)
              }
          }

          val globalBCoords = localCoords.globalNeighbor(side, chunkCoords)

          var brSum = 0f
          var brCount = 0
          var bIdx = 0
          val bLen = b.length
          while bIdx < bLen do {
            val br = brightness(globalBCoords.offset(b(bIdx)))
            if br != 0 then {
              brSum += br
              brCount += 1
            }
            bIdx += 1
          }

          buf.putFloat(if brCount == 0 then brightness(globalBCoords) else brSum / brCount)
          i2 += 1
        }
      }

      i1 += 1
    }
  }

  private def oppositeSide(s: Int): Int = {
    if s < 2 then {
      1 - s
    } else {
      (s - 2 + 3) % 3 + 2
    }
  }

package hexacraft.world.render

import hexacraft.renderer.TextureArray
import hexacraft.world.Camera
import hexacraft.world.coord.ChunkRelWorld

import org.joml.Vector3f

import java.nio.ByteBuffer

class ChunkRenderHandler:
  private val blockShader = new BlockShader(isSide = false)
  private val blockSideShader = new BlockShader(isSide = true)
  private val blockTexture = TextureArray.getTextureArray("blocks")

  private val regularBlockHexagonHandler = new HexagonRenderHandler(blockShader, blockSideShader)
  private val transmissiveBlockHexagonHandler = new HexagonRenderHandler(blockShader, blockSideShader)

  def regularChunkBufferFragmentation: IndexedSeq[Float] = regularBlockHexagonHandler.fragmentation
  def transmissiveChunkBufferFragmentation: IndexedSeq[Float] = transmissiveBlockHexagonHandler.fragmentation

  def onTotalSizeChanged(totalSize: Int): Unit =
    blockShader.setTotalSize(totalSize)
    blockSideShader.setTotalSize(totalSize)

  def onProjMatrixChanged(camera: Camera): Unit =
    blockShader.setProjectionMatrix(camera.proj.matrix)
    blockSideShader.setProjectionMatrix(camera.proj.matrix)

  def render(camera: Camera, sun: Vector3f): Unit =
    blockShader.setViewMatrix(camera.view.matrix)
    blockShader.setCameraPosition(camera.position)
    blockShader.setSunPosition(sun)

    blockSideShader.setViewMatrix(camera.view.matrix)
    blockSideShader.setCameraPosition(camera.position)
    blockSideShader.setSunPosition(sun)

    blockTexture.bind()
    regularBlockHexagonHandler.render()
    transmissiveBlockHexagonHandler.render()

  def setChunkRenderData(
      coords: ChunkRelWorld,
      opaqueBlocks: IndexedSeq[ByteBuffer],
      transmissiveBlocks: IndexedSeq[ByteBuffer]
  ): Unit =
    regularBlockHexagonHandler.setChunkContent(coords, opaqueBlocks)
    transmissiveBlockHexagonHandler.setChunkContent(coords, transmissiveBlocks)

  def clearChunkRenderData(coords: ChunkRelWorld): Unit =
    regularBlockHexagonHandler.clearChunkContent(coords)
    transmissiveBlockHexagonHandler.clearChunkContent(coords)

  def unload(): Unit =
    regularBlockHexagonHandler.unload()
    transmissiveBlockHexagonHandler.unload()
    blockShader.free()
    blockSideShader.free()

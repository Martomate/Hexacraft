package hexacraft.world.render

import hexacraft.renderer.TextureArray
import hexacraft.world.camera.Camera
import hexacraft.world.coord.integer.ChunkRelWorld
import hexacraft.world.render.aspect.HexagonRenderHandler

import org.joml.Vector3f

class ChunkRenderHandler:
  private val blockShader = new BlockShader(isSide = false)
  private val blockSideShader = new BlockShader(isSide = true)
  private val blockTexture = TextureArray.getTextureArray("blocks")

  private val regularBlockHexagonHandler = new HexagonRenderHandler(blockShader, blockSideShader)
  private val transmissiveBlockHexagonHandler = new HexagonRenderHandler(blockShader, blockSideShader)

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
      opaqueBlocks: ChunkRenderData,
      transmissiveBlocks: ChunkRenderData
  ): Unit =
    regularBlockHexagonHandler.setChunkContent(coords, opaqueBlocks.blockDataPerSide)
    transmissiveBlockHexagonHandler.setChunkContent(coords, transmissiveBlocks.blockDataPerSide)

  def clearChunkRenderData(coords: ChunkRelWorld): Unit =
    regularBlockHexagonHandler.clearChunkContent(coords)
    transmissiveBlockHexagonHandler.clearChunkContent(coords)

  def unload(): Unit =
    regularBlockHexagonHandler.unload()
    transmissiveBlockHexagonHandler.unload()
    blockShader.free()
    blockSideShader.free()

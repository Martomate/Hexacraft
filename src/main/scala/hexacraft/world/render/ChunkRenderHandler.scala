package hexacraft.world.render

import hexacraft.renderer.{Texture, TextureArray}
import hexacraft.world.camera.Camera
import hexacraft.world.coord.integer.ChunkRelWorld
import hexacraft.world.render.aspect.HexagonRenderHandler

import org.joml.Vector3f

import scala.collection.mutable

class ChunkRenderHandler:
  private val hexagonHandlers: mutable.Map[Texture, HexagonRenderHandler] = mutable.Map.empty

  private val blockShader = new BlockShader(isSide = false)
  private val blockSideShader = new BlockShader(isSide = true)
  private val blockTexture = TextureArray.getTextureArray("blocks")

  private val blockHexagonHandler = new HexagonRenderHandler(blockShader, blockSideShader)
  hexagonHandlers(blockTexture) = blockHexagonHandler

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

    for (t, r) <- hexagonHandlers do
      t.bind()
      r.render()

  def setChunkRenderData(coords: ChunkRelWorld, data: ChunkRenderData): Unit =
    blockHexagonHandler.setChunkContent(coords, data.blockSide)

  def clearChunkRenderData(coords: ChunkRelWorld): Unit =
    blockHexagonHandler.clearChunkContent(coords)

  def unload(): Unit =
    for h <- hexagonHandlers.values do h.unload()
    hexagonHandlers.clear()
    blockShader.free()
    blockSideShader.free()

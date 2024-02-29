package hexacraft.world.render

import hexacraft.renderer.{GpuState, TextureArray}
import hexacraft.shaders.block.BlockShader
import hexacraft.world.Camera
import hexacraft.world.coord.ChunkRelWorld

import org.joml.Vector3f

import java.nio.ByteBuffer

class ChunkRenderHandler {
  private val blockShader = new BlockShader(isSide = false)
  private val blockSideShader = new BlockShader(isSide = true)
  private val blockTexture = TextureArray.getTextureArray("blocks")

  private val regularBlockHexagonHandler = new HexagonRenderHandler(
    blockShader,
    blockSideShader,
    GpuState.build(_.blend(false).cullFace(true))
  )
  private val transmissiveBlockHexagonHandler = new HexagonRenderHandler(
    blockShader,
    blockSideShader,
    GpuState.build(_.blend(true).cullFace(false))
  )

  def regularChunkBufferFragmentation: IndexedSeq[Float] = regularBlockHexagonHandler.fragmentation

  def transmissiveChunkBufferFragmentation: IndexedSeq[Float] = transmissiveBlockHexagonHandler.fragmentation

  def onTotalSizeChanged(totalSize: Int): Unit = {
    blockShader.setTotalSize(totalSize)
    blockSideShader.setTotalSize(totalSize)
  }

  def onProjMatrixChanged(camera: Camera): Unit = {
    blockShader.setProjectionMatrix(camera.proj.matrix)
    blockSideShader.setProjectionMatrix(camera.proj.matrix)
  }

  def render(camera: Camera, sun: Vector3f): Unit = {
    blockShader.setViewMatrix(camera.view.matrix)
    blockShader.setCameraPosition(camera.position)
    blockShader.setSunPosition(sun)

    blockSideShader.setViewMatrix(camera.view.matrix)
    blockSideShader.setCameraPosition(camera.position)
    blockSideShader.setSunPosition(sun)

    blockTexture.bind()
    regularBlockHexagonHandler.render()
    transmissiveBlockHexagonHandler.render()
  }

  def update(chunks: Seq[(ChunkRelWorld, ChunkRenderData)]): Unit = {
    val opaqueData = chunks.partitionMap((coords, data) =>
      data.opaqueBlocks match {
        case Some(content) => Right((coords, content))
        case None          => Left(coords)
      }
    )
    regularBlockHexagonHandler.update(opaqueData._1, opaqueData._2)

    val transmissiveData = chunks.partitionMap((coords, data) =>
      data.transmissiveBlocks match {
        case Some(content) => Right((coords, content))
        case None          => Left(coords)
      }
    )
    transmissiveBlockHexagonHandler.update(transmissiveData._1, transmissiveData._2)
  }

  def unload(): Unit = {
    regularBlockHexagonHandler.unload()
    transmissiveBlockHexagonHandler.unload()
    blockShader.free()
    blockSideShader.free()
  }
}

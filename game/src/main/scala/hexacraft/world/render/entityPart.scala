package hexacraft.world.render

import hexacraft.infra.gpu.OpenGL
import hexacraft.renderer.{Shader, ShaderConfig, VAO}
import hexacraft.world.{BlocksInWorld, ChunkCache, CylinderSize}
import hexacraft.world.coord.{CoordUtils, CylCoords}
import hexacraft.world.entity.{Entity, EntityModel}

import org.joml.{Matrix4f, Vector3d, Vector3f, Vector4f}

import java.nio.ByteBuffer

class EntityShader(isSide: Boolean) {
  private val config = ShaderConfig("entity_part")
    .withInputs(
      "position",
      "texCoords",
      "normal",
      "vertexIndex",
      "faceIndex",
      "modelMatrix",
      "",
      "",
      "",
      "texOffset",
      "texDim",
      "blockTex",
      "brightness"
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

  def setTextureSize(texSize: Int): Unit = {
    shader.setUniform1i("texSize", texSize)
  }

  def enable(): Unit = {
    shader.activate()
  }

  def free(): Unit = {
    shader.free()
  }
}

object EntityPartVao {
  def forSide(side: Int): VAO = {
    VAO
      .builder()
      .addVertexVbo(BlockRenderer.verticesPerInstance(side), OpenGL.VboUsage.StaticDraw)(
        _.floats(0, 3)
          .floats(1, 2)
          .floats(2, 3)
          .ints(3, 1)
          .ints(4, 1),
        _.fill(0, BlockRenderer.setupBlockVBO(side))
      )
      .addInstanceVbo(0, OpenGL.VboUsage.DynamicDraw)(
        _.floatsArray(5, 4)(4)
          .ints(9, 2)
          .ints(10, 2)
          .ints(11, 1)
          .floats(12, 1)
      )
      .finish(BlockRenderer.verticesPerInstance(side))
  }
}

case class EntityDataForShader(model: EntityModel, parts: Seq[EntityPartDataForShader])

case class EntityPartDataForShader(
    modelMatrix: Matrix4f,
    texOffset: (Int, Int),
    texSize: (Int, Int),
    blockTex: Int,
    brightness: Float
) {
  def fill(buf: ByteBuffer): Unit = {
    modelMatrix.get(buf)
    buf.position(buf.position() + 16 * 4)
    buf.putInt(texOffset._1)
    buf.putInt(texOffset._2)
    buf.putInt(texSize._1)
    buf.putInt(texSize._2)
    buf.putInt(blockTex)
    buf.putFloat(brightness)
  }
}

object EntityRenderDataFactory {
  def getEntityRenderData(entities: Iterable[Entity], side: Int, world: BlocksInWorld)(using
      CylinderSize
  ): Iterable[EntityDataForShader] = {
    val chunkCache = new ChunkCache(world)

    val tr = new Matrix4f

    for ent <- entities if ent.model.isDefined yield {
      val baseT = ent.transform.transform
      val model = ent.model.get

      val parts = for part <- model.parts yield {
        baseT.mul(part.transform, tr)

        val coords4 = tr.transform(new Vector4f(0, 0.5f, 0, 1))
        val blockCoords = CylCoords(coords4.x, coords4.y, coords4.z).toBlockCoords
        val coords = CoordUtils.getEnclosingBlock(blockCoords)._1
        val cCoords = coords.getChunkRelWorld

        val partChunk = chunkCache.getChunk(cCoords)

        val brightness: Float =
          if partChunk != null then {
            partChunk.lighting.getBrightness(coords.getBlockRelChunk)
          } else {
            0
          }

        EntityPartDataForShader(
          modelMatrix = new Matrix4f(tr),
          texOffset = part.textureOffset(side),
          texSize = part.textureSize(side),
          blockTex = part.texture(side),
          brightness
        )
      }

      EntityDataForShader(model, parts)
    }
  }
}

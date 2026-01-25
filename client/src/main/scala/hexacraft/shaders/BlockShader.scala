package hexacraft.shaders

import hexacraft.infra.gpu.OpenGL
import hexacraft.infra.gpu.OpenGL.ShaderType
import hexacraft.renderer.{Shader, ShaderConfig, VAO}

import org.joml.*

import java.nio.ByteBuffer

class BlockShader(isSide: Boolean) {
  private val shader = Shader.from(
    ShaderConfig()
      .withStage(ShaderType.Vertex, "block/vert.glsl")
      .withStage(ShaderType.Fragment, "block/frag.glsl")
      .withInputs("position", "texIndex", "normal", "brightness", "texCoords")
      .withDefines("isSide" -> (if isSide then "1" else "0"))
  )

  def setTotalSize(totalSize: Int): Unit = {
    shader.setUniform1i("totalSize", totalSize)
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

object BlockShader {
  def createVao(side: Int, maxVertices: Int): VAO = {
    val verticesPerInstance = verticesPerBlock(side)

    VAO.build(maxVertices)(
      _.addVertexVbo(maxVertices, OpenGL.VboUsage.DynamicDraw)(
        _.ints(0, 3)
          .ints(1, 1)
          .floats(2, 3)
          .floats(3, 1)
          .floats(4, 2)
      )
    )
  }

  def verticesPerBlock(side: Int): Int = {
    if side < 2 then {
      3 * 6
    } else {
      3 * 2
    }
  }

  def bytesPerVertex(side: Int): Int = (4 + 6) * 4

  case class BlockVertexData(
      position: Vector3i,
      texIndex: Int,
      normal: Vector3f,
      brightness: Float,
      texCoords: Vector2f
  ) {
    def fill(buf: ByteBuffer): Unit = {
      buf.putInt(position.x)
      buf.putInt(position.y)
      buf.putInt(position.z)

      buf.putInt(texIndex)

      buf.putFloat(normal.x)
      buf.putFloat(normal.y)
      buf.putFloat(normal.z)

      buf.putFloat(brightness)

      buf.putFloat(texCoords.x)
      buf.putFloat(texCoords.y)
    }
  }
}

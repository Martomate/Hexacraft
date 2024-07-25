package hexacraft.shaders

import hexacraft.infra.gpu.OpenGL
import hexacraft.infra.gpu.OpenGL.ShaderType
import hexacraft.renderer.{Shader, ShaderConfig, VAO}

import org.joml.*

import java.nio.ByteBuffer

class TerrainShader {
  private val shader = Shader.from(
    ShaderConfig()
      .withStage(ShaderType.Vertex, "terrain/vert.glsl")
      .withStage(ShaderType.Fragment, "terrain/frag.glsl")
      .withInputs("position", "color")
  )

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

  def enable(): Unit = {
    shader.activate()
  }

  def free(): Unit = {
    shader.free()
  }
}

object TerrainShader {
  def createVao(maxVertices: Int): VAO = {
    VAO.build(maxVertices)(
      _.addVertexVbo(maxVertices, OpenGL.VboUsage.DynamicDraw)(
        _.ints(0, 3).floats(1, 3)
      )
    )
  }

  def bytesPerVertex: Int = 6 * 4

  class TerrainVertexData(position: Vector3i, color: Vector3f) {
    def fill(buf: ByteBuffer): Unit = {
      buf.putInt(position.x)
      buf.putInt(position.y)
      buf.putInt(position.z)

      buf.putFloat(color.x)
      buf.putFloat(color.y)
      buf.putFloat(color.z)
    }
  }
}

package hexacraft.shaders

import hexacraft.infra.gpu.OpenGL
import hexacraft.infra.gpu.OpenGL.ShaderType.{Fragment, Vertex}
import hexacraft.renderer.*

import org.joml.{Matrix4f, Vector3f, Vector4fc}

class SkyShader {
  private val shader = Shader.from(
    ShaderConfig()
      .withStage(Vertex, "sky/vert.glsl")
      .withStage(Fragment, "sky/frag.glsl")
      .withInputs("position")
  )

  WaterFog.setConstants(shader)

  def setInverseProjectionMatrix(matrix: Matrix4f): Unit = {
    shader.setUniformMat4("invProjMatr", matrix)
  }

  def setInverseViewMatrix(matrix: Matrix4f): Unit = {
    shader.setUniformMat4("invViewMatr", matrix)
  }

  def setSunPosition(sun: Vector3f): Unit = {
    shader.setUniform3f("sun", sun.x, sun.y, sun.z)
  }

  def setTotalSize(totalSize: Int): Unit = {
    shader.setUniform1i("totalSize", totalSize)
  }

  /** @param hasOcean if the world has an ocean (at sea level), which will then be drawn below the horizon
    * @param waterSurfaceColor the average color and alpha of the texture of the water surface
    */
  def setOcean(hasOcean: Boolean, waterSurfaceColor: Vector4fc): Unit = {
    shader.setUniform1i("hasOcean", if hasOcean then 1 else 0)
    shader.setUniform4f(
      "waterSurfaceColor",
      waterSurfaceColor.x,
      waterSurfaceColor.y,
      waterSurfaceColor.z,
      waterSurfaceColor.w
    )
  }

  def setSeaLevelAboveEye(height: Float): Unit = {
    shader.setUniform1f("seaLevelAboveEye", height)
  }

  /** @param surfaceAboveEye the height of the water surface relative to the eye
    * @param strength how strong the water fog should be (0 means no fog)
    */
  def setWaterSurface(surfaceAboveEye: Float, strength: Float): Unit = {
    WaterFog.setSurface(shader, surfaceAboveEye, strength)
  }

  def enable(): Unit = {
    shader.activate()
  }

  def free(): Unit = {
    shader.free()
  }
}

object SkyShader {
  def createVao(): VAO = {
    VAO.build(4)(
      _.addVertexVbo(4)(
        _.floats(0, 2),
        _.fillFloats(0, Seq(-1, -1, 1, -1, -1, 1, 1, 1))
      )
    )
  }

  def createRenderer(): Renderer = new Renderer(OpenGL.PrimitiveMode.TriangleStrip, GpuState.build(_.depthTest(false)))
}

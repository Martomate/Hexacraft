package hexacraft.shaders

import hexacraft.infra.gpu.OpenGL
import hexacraft.infra.gpu.OpenGL.ShaderType.{Fragment, Vertex}
import hexacraft.renderer.{InstancedRenderer, Shader, ShaderConfig, VAO}
import hexacraft.world.block.BlockState
import hexacraft.world.coord.{BlockRelWorld, CylCoords}
import org.joml.{Matrix4f, Vector3d}

import java.nio.ByteBuffer

class SelectedBlockShader {
  private val shader = Shader.from(
    ShaderConfig()
      .withStage(Vertex, "selected_block/vert.glsl")
      .withStage(Fragment, "selected_block/frag.glsl")
      .withInputs("position", "blockPos", "color", "blockHeight")
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

  def enable(): Unit = {
    shader.activate()
  }

  def free(): Unit = {
    shader.free()
  }
}

object SelectedBlockShader {
  def createVao(): VAO = {
    def expandFn(v: CylCoords.Offset): Seq[Float] = Seq(
      v.x * 1.0025,
      (v.y - 0.25) * 1.0025 + 0.25,
      v.z * 1.0025
    ).map(_.toFloat)

    def fn(s: Int): Seq[Float] = BlockState.getVertices(s + 2).flatMap(expandFn)

    val vertexData = Seq(
      Seq(0, 2, 4).flatMap(fn),
      expandFn(BlockState.vertices.head),
      Seq(1, 3, 5).flatMap(fn)
    ).reduce(_ ++ _)

    VAO
      .builder()
      .addVertexVbo(25)(
        _.floats(0, 3),
        _.fillFloats(0, vertexData)
      )
      .addInstanceVbo(1, OpenGL.VboUsage.DynamicDraw)(
        _.ints(1, 3).floats(2, 3).floats(3, 1)
      )
      .finish(25)
  }

  def createRenderer(): InstancedRenderer = new InstancedRenderer(OpenGL.PrimitiveMode.LineStrip)

  class InstanceData(coords: BlockRelWorld, blockState: BlockState) {
    def fill(buf: ByteBuffer): Unit = {
      buf.putInt(coords.x)
      buf.putInt(coords.y)
      buf.putInt(coords.z)
      buf.putFloat(0)
      buf.putFloat(0)
      buf.putFloat(0)
      buf.putFloat(blockState.blockType.blockHeight(blockState.metadata))
    }
  }
}

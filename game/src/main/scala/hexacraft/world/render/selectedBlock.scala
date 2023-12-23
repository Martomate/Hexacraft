package hexacraft.world.render

import hexacraft.infra.gpu.OpenGL
import hexacraft.renderer.{Shader, ShaderConfig, VAO}
import hexacraft.world.block.BlockState
import hexacraft.world.coord.{BlockRelWorld, CylCoords}

import org.joml.{Matrix4f, Vector3d}
import org.lwjgl.BufferUtils

class SelectedBlockShader {
  private val config = ShaderConfig("selected_block")
    .withAttribs("position", "blockPos", "color", "blockHeight")

  private val shader = Shader.from(config)

  def setTotalSize(totalSize: Int): Unit =
    shader.setUniform1i("totalSize", totalSize)

  def setCameraPosition(cam: Vector3d): Unit =
    shader.setUniform3f("cam", cam.x.toFloat, cam.y.toFloat, cam.z.toFloat)

  def setProjectionMatrix(matrix: Matrix4f): Unit =
    shader.setUniformMat4("projMatrix", matrix)

  def setViewMatrix(matrix: Matrix4f): Unit =
    shader.setUniformMat4("viewMatrix", matrix)

  def enable(): Unit = shader.activate()

  def free(): Unit = shader.free()
}

object SelectedBlockVao {
  def create: SelectedBlockVao =
    def expandFn(v: CylCoords.Offset): Seq[Float] =
      Seq(v.x * 1.0025, (v.y - 0.25) * 1.0025 + 0.25, v.z * 1.0025).map(_.toFloat)

    def fn(s: Int): Seq[Float] = BlockState.getVertices(s + 2).flatMap(expandFn)

    val vertexData = Seq(0, 2, 4).flatMap(fn) ++ expandFn(BlockState.vertices.head) ++ Seq(1, 3, 5).flatMap(fn)

    val vao = VAO
      .builder()
      .addVertexVbo(25)(
        _.floats(0, 3),
        _.fillFloats(0, vertexData)
      )
      .addInstanceVbo(1, OpenGL.VboUsage.DynamicDraw)(
        _.ints(1, 3).floats(2, 3).floats(3, 1)
      )
      .finish(25)

    new SelectedBlockVao(vao)
}

class SelectedBlockVao private (val vao: VAO) {
  def setSelectedBlock(coords: BlockRelWorld, blockState: BlockState): Unit =
    val buf = BufferUtils
      .createByteBuffer(7 * 4)
      .putInt(coords.x)
      .putInt(coords.y)
      .putInt(coords.z)
      .putFloat(0)
      .putFloat(0)
      .putFloat(0)
      .putFloat(blockState.blockType.blockHeight(blockState.metadata))

    buf.flip()
    vao.vbos(1).fill(0, buf)

  def free(): Unit = vao.free()
}

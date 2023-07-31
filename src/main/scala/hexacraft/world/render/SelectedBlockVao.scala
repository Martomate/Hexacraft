package hexacraft.world.render

import hexacraft.infra.gpu.OpenGL
import hexacraft.renderer.VAO
import hexacraft.world.block.BlockState
import hexacraft.world.coord.fp.CylCoords
import hexacraft.world.coord.integer.BlockRelWorld

import org.lwjgl.BufferUtils

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

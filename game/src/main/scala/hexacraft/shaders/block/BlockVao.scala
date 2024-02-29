package hexacraft.shaders.block

import hexacraft.infra.gpu.OpenGL
import hexacraft.renderer.VAO

object BlockVao {
  def verticesPerBlock(side: Int): Int = {
    if side < 2 then {
      3 * 6
    } else {
      3 * 2
    }
  }

  def bytesPerVertex(side: Int): Int = (4 + 6) * 4

  def forSide(side: Int)(maxVertices: Int): VAO = {
    val verticesPerInstance = BlockVao.verticesPerBlock(side)

    VAO
      .builder()
      .addVertexVbo(maxVertices, OpenGL.VboUsage.DynamicDraw)(
        _.ints(0, 3)
          .ints(1, 1)
          .floats(2, 3)
          .floats(3, 1)
          .floats(4, 2)
      )
      .finish(maxVertices)
  }
}

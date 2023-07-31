package hexacraft.world.render

import hexacraft.renderer.VAO

object WorldCombinerVao {
  def create: VAO =
    VAO
      .builder()
      .addVertexVbo(4)(
        _.floats(0, 2),
        _.fillFloats(0, Seq(-1, -1, 1, -1, -1, 1, 1, 1))
      )
      .finish(4)
}

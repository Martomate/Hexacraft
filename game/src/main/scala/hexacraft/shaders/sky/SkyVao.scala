package hexacraft.shaders.sky

import hexacraft.renderer.VAO

object SkyVao {
  def create: VAO = {
    VAO
      .builder()
      .addVertexVbo(4)(
        _.floats(0, 2),
        _.fillFloats(0, Seq(-1, -1, 1, -1, -1, 1, 1, 1))
      )
      .finish(4)
  }
}

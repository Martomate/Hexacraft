package hexacraft.world.render

import hexacraft.infra.gpu.OpenGL
import hexacraft.renderer.VAO

object EntityPartVao {
  def forSide(side: Int): VAO =
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
      .finish(BlockRenderer.verticesPerInstance(side), 0)
}
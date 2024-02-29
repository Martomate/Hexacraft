package hexacraft.shaders.gui_block

import hexacraft.infra.gpu.OpenGL
import hexacraft.renderer.VAO
import hexacraft.world.render.BlockRenderer

object GuiBlockVao {
  def forSide(side: Int): VAO = {
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
        _.floats(5, 2)
          .ints(6, 1)
          .floats(7, 1)
          .floats(8, 1)
      )
      .finish(BlockRenderer.verticesPerInstance(side))
  }
}

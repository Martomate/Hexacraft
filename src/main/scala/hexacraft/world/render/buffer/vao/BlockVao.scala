package hexacraft.world.render.buffer.vao

import hexacraft.infra.gpu.OpenGL
import hexacraft.renderer.VAO
import hexacraft.world.render.BlockRenderer

object BlockVao {
  private def verticesPerInstance(side: Int): Int = if side < 2 then 3 * 6 else 3 * 2
  private def brightnessesPerInstance(side: Int): Int = if side < 2 then 7 else 4

  def bytesPerInstance(side: Int): Int = (5 + BlockVao.brightnessesPerInstance(side)) * 4

  def forSide(side: Int)(maxInstances: Int): VAO =
    val verticesPerInstance = BlockVao.verticesPerInstance(side)
    val brightnessesPerInstance = BlockVao.brightnessesPerInstance(side)

    VAO
      .builder()
      .addVertexVbo(verticesPerInstance, OpenGL.VboUsage.StaticDraw)(
        _.floats(0, 3)
          .floats(1, 2)
          .floats(2, 3)
          .ints(3, 1)
          .ints(4, 1),
        _.fill(0, BlockRenderer.setupBlockVBO(side))
      )
      .addInstanceVbo(maxInstances, OpenGL.VboUsage.DynamicDraw)(
        _.ints(5, 3)
          .ints(6, 1)
          .floats(7, 1)
          .floatsArray(8, 1)(brightnessesPerInstance)
      )
      .finish(verticesPerInstance, maxInstances)
}

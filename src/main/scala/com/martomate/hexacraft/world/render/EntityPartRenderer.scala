package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.renderer.{InstancedRenderer, VAO}
import com.martomate.hexacraft.util.OpenGL

object EntityPartRenderer:
  def forSide(side: Int): BlockRenderer =
    val vao = initVAO(side)
    val renderer = makeRenderer(vao)
    BlockRenderer(side, vao, renderer)

  private def initVAO(side: Int): VAO =
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

  private def makeRenderer(vao: VAO) = new InstancedRenderer(vao, OpenGL.PrimitiveMode.Triangles)

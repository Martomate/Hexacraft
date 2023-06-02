package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.renderer.{InstancedRenderer, VAO, VAOBuilder, VBOBuilder}
import com.martomate.hexacraft.util.OpenGL

object EntityPartRenderer:
  def forSide(side: Int): BlockRenderer =
    val vao = initVAO(side)
    val renderer = makeRenderer(vao)
    BlockRenderer(side, vao, renderer)

  private def initVAO(side: Int): VAO =
    new VAOBuilder(BlockRenderer.verticesPerInstance(side), 0)
      .addVBO(
        VBOBuilder(BlockRenderer.verticesPerInstance(side), OpenGL.VboUsage.StaticDraw)
          .floats(0, 3)
          .floats(1, 2)
          .floats(2, 3)
          .ints(3, 1)
          .ints(4, 1)
          .create()
          .fill(0, BlockRenderer.setupBlockVBO(side))
      )
      .addVBO(
        VBOBuilder(0, OpenGL.VboUsage.DynamicDraw, 1)
          .floats(5, 4)
          .floats(6, 4)
          .floats(7, 4)
          .floats(8, 4)
          .ints(9, 2)
          .ints(10, 2)
          .ints(11, 1)
          .floats(12, 1)
          .create()
      )
      .create()

  private def makeRenderer(vao: VAO) = new InstancedRenderer(vao, OpenGL.PrimitiveMode.Triangles)

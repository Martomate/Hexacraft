package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.renderer.*
import com.martomate.hexacraft.util.OpenGL

object FlatBlockRenderer:
  def forSide(side: Int): BlockRenderer =
    val vao = initVAO(side)
    val renderer = makeRenderer(vao)
    new BlockRenderer(side, vao, renderer)

  private def initVAO(side: Int): VAO =
    new VAOBuilder(BlockRenderer.verticesPerInstance(side), 0)
      .addVBO(
        VBOBuilder()
          .floats(0, 3)
          .floats(1, 2)
          .floats(2, 3)
          .ints(3, 1)
          .ints(4, 1)
          .create(BlockRenderer.verticesPerInstance(side), OpenGL.VboUsage.StaticDraw)
          .fill(0, BlockRenderer.setupBlockVBO(side))
      )
      .addVBO(
        VBOBuilder()
          .floats(5, 2)
          .ints(6, 1)
          .floats(7, 1)
          .floats(8, 1)
          .create(0, OpenGL.VboUsage.DynamicDraw, 1)
      )
      .create()

  private def makeRenderer(vao: VAO): Renderer = new InstancedRenderer(vao, OpenGL.PrimitiveMode.Triangles)
    with NoDepthTest

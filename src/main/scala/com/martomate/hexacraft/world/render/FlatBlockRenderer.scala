package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.renderer.*

import org.lwjgl.opengl.{GL11, GL15}

object FlatBlockRenderer:
  def forSide(side: Int): BlockRenderer =
    val vao = initVAO(side)
    val renderer = makeRenderer(vao)
    new BlockRenderer(side, vao, renderer)

  private def initVAO(side: Int): VAO =
    new VAOBuilder(BlockRenderer.verticesPerInstance(side), 0)
      .addVBO(
        VBOBuilder(BlockRenderer.verticesPerInstance(side), GL15.GL_STATIC_DRAW)
          .floats(0, 3)
          .floats(1, 2)
          .floats(2, 3)
          .ints(3, 1)
          .create()
          .fill(0, BlockRenderer.setupBlockVBO(side))
      )
      .addVBO(
        VBOBuilder(0, GL15.GL_DYNAMIC_DRAW, 1)
          .floats(4, 2)
          .ints(5, 1)
          .floats(6, 1)
          .floats(7, 1)
          .create()
      )
      .create()

  private def makeRenderer(vao: VAO): Renderer = new InstancedRenderer(vao, GL11.GL_TRIANGLES) with NoDepthTest

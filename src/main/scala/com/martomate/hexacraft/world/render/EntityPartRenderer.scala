package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.renderer.{InstancedRenderer, VAO, VAOBuilder, VBOBuilder}

import org.lwjgl.opengl.{GL11, GL15}

object EntityPartRenderer:
  def forSide(side: Int): BlockRenderer =
    val vao = initVAO(side)
    val renderer = makeRenderer(vao)
    BlockRenderer(side, vao, renderer)

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
          .floats(4, 4)
          .floats(5, 4)
          .floats(6, 4)
          .floats(7, 4)
          .ints(8, 2)
          .ints(9, 2)
          .ints(10, 1)
          .floats(11, 1)
          .create()
      )
      .create()

  private def makeRenderer(vao: VAO) = new InstancedRenderer(vao, GL11.GL_TRIANGLES)

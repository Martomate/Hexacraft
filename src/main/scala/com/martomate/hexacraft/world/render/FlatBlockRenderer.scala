package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.renderer._
import org.lwjgl.opengl.{GL11, GL15}

class FlatBlockRenderer(_side: Int, _init_maxInstances: Int) extends BlockRenderer(_side, _init_maxInstances) {
  override protected def initVAO(): VAO = new VAOBuilder(verticesPerInstance, maxInstances)
    .addVBO(VBOBuilder(verticesPerInstance, GL15.GL_STATIC_DRAW)
      .floats(0, 3)
      .floats(1, 2)
      .floats(2, 3)
      .ints(3, 1)
      .create().fill(0, setupBlockVBO(side)))
    .addVBO(VBOBuilder(maxInstances, GL15.GL_DYNAMIC_DRAW, 1)
      .floats(4, 2)
      .ints(5, 1)
      .floats(6, 1)
      .floats(7, 1)
      .create())
    .create()

  override val renderer: Renderer = new InstancedRenderer(vao, GL11.GL_TRIANGLE_STRIP) with NoDepthTest
}

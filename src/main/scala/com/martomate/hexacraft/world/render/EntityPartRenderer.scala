package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.renderer.{InstancedRenderer, VAO, VAOBuilder, VBOBuilder}
import org.lwjgl.opengl.{GL11, GL15}

class EntityPartRenderer(_side: Int, _init_maxInstances: Int) extends BlockRenderer(_side, _init_maxInstances) {
  override protected def initVAO(): VAO = new VAOBuilder(verticesPerInstance, maxInstances)
    .addVBO(VBOBuilder(verticesPerInstance, GL15.GL_STATIC_DRAW)
      .floats(0, 3)
      .floats(1, 2)
      .floats(2, 3)
      .ints(3, 1)
      .create().fill(0, setupBlockVBO(side)))
    .addVBO(VBOBuilder(maxInstances, GL15.GL_DYNAMIC_DRAW, 1)
      .floats(4, 4)
      .floats(5, 4)
      .floats(6, 4)
      .floats(7, 4)
      .ints(8, 2)
      .ints(9, 2)
      .ints(10, 1)
      .floats(11, 1)
      .create())
    .create()

  override val renderer = new InstancedRenderer(vao, GL11.GL_TRIANGLE_STRIP)
}

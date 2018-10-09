package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.renderer._
import org.lwjgl.opengl.{GL11, GL15}

class FlatBlockRenderer(_side: Int, _init_maxInstances: Int) extends BlockRenderer(_side, _init_maxInstances) {
  override val vao: VAO = new VAOBuilder(verticesPerInstance, maxInstances)
    .addVBO(VBOBuilder(if (side < 2) 6 else 4, GL15.GL_STATIC_DRAW).floats(0, 3)
      .floats(1, 2)
      .floats(2, 3)
      .create().fillFloats(0, setupBlockVBO(side)))
    .addVBO(VBOBuilder(maxInstances, GL15.GL_DYNAMIC_DRAW, 1)
      .floats(3, 2)
      .ints(4, 1)
      .floats(5, 1)
      .floats(6, 1)
      .create())
    .create()

  override val renderer = new InstancedRenderer(vao, GL11.GL_TRIANGLE_STRIP) with NoDepthTest
}

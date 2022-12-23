package com.martomate.hexacraft.world.render.buffer.vao

import com.martomate.hexacraft.renderer.{VAO, VAOBuilder, VBOBuilder}
import com.martomate.hexacraft.world.render.{BlockRenderer, BlockVertexData}
import com.martomate.hexacraft.world.render.buffer.RenderBufferFactory

import org.joml.{Vector2f, Vector3f}
import org.lwjgl.opengl.{GL11, GL15}

class BlockVAORenderBufferFactory(side: Int) extends RenderBufferFactory[VAORenderBuffer] {
  private val verticesPerInstance: Int = if (side < 2) 3 * 6 else 3 * 2
  private val brightnessesPerInstance: Int = if (side < 2) 7 else 4

  override def bytesPerInstance: Int = (5 + brightnessesPerInstance) * 4

  override def makeBuffer(maxInstances: Int): VAORenderBuffer = {
    new VAORenderBuffer(makeVAO(maxInstances), 1, GL11.GL_TRIANGLES)
  }

  private def makeVAO(maxInstances: Int): VAO = new VAOBuilder(verticesPerInstance, maxInstances)
    .addVBO(
      VBOBuilder(verticesPerInstance, GL15.GL_STATIC_DRAW)
        .floats(0, 3)
        .floats(1, 2)
        .floats(2, 3)
        .ints(3, 1)
        .create()
        .fill(0, BlockRenderer.setupBlockVBO(side))
    )
    .addVBO(
      VBOBuilder(maxInstances, GL15.GL_DYNAMIC_DRAW, 1) // GL_DYNAMIC_DRAW
        .ints(4, 3)
        .ints(5, 1)
        .floats(6, 1)
        .floatsArray(7, 1)(
          brightnessesPerInstance
        ) // after this index should be 'this index' + brightnessesPerInstance
        .create()
    )
    .create()
}

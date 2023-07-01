package com.martomate.hexacraft.world.render.buffer.vao

import com.martomate.hexacraft.infra.OpenGL
import com.martomate.hexacraft.renderer.VAO
import com.martomate.hexacraft.world.render.{BlockRenderer, BlockVertexData}
import com.martomate.hexacraft.world.render.buffer.RenderBufferFactory

import org.joml.{Vector2f, Vector3f}

class BlockVAORenderBufferFactory(side: Int) extends RenderBufferFactory[VAORenderBuffer] {
  private val verticesPerInstance: Int = if (side < 2) 3 * 6 else 3 * 2
  private val brightnessesPerInstance: Int = if (side < 2) 7 else 4

  override def bytesPerInstance: Int = (5 + brightnessesPerInstance) * 4

  override def makeBuffer(maxInstances: Int): VAORenderBuffer = {
    new VAORenderBuffer(makeVAO(maxInstances), 1, OpenGL.PrimitiveMode.Triangles)
  }

  private def makeVAO(maxInstances: Int): VAO = VAO
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

package com.martomate.hexacraft.world.render.buffer.vao

import com.martomate.hexacraft.renderer.{VAO, VAOBuilder, VBOBuilder}
import com.martomate.hexacraft.world.render.BlockVertexData
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
        .fill(0, setupBlockVBO(side))
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

  protected def setupBlockVBO(s: Int): Seq[BlockVertexData] = {
    if (s < 2) {
      val ints = Seq(6, 0, 1, 6, 1, 2, 6, 2, 3, 6, 3, 4, 6, 4, 5, 6, 5, 0)

      for (i <- 0 until verticesPerInstance) yield {
        val a = ints(if (s == 1) i else verticesPerInstance - 1 - i)

        val (x, z) =
          if a == 6 then (0f, 0f)
          else
            val v = a * Math.PI / 3
            (Math.cos(v).toFloat, Math.sin(v).toFloat)

        BlockVertexData(
          new Vector3f(x, (1 - s).toFloat, z),
          new Vector2f((1 + (if (s == 0) -x else x)) / 2, (1 + z) / 2),
          new Vector3f(0, (1 - 2 * s).toFloat, 0),
          a
        )
      }
    } else {
      val ints = Seq(0, 1, 3, 2, 0, 3)

      val nv = ((s - 1) % 6 - 0.5) * Math.PI / 3
      val nx = Math.cos(nv).toFloat
      val nz = Math.sin(nv).toFloat

      for (i <- 0 until verticesPerInstance) yield {
        val a = ints(i)
        val v = (s - 2 + a % 2) % 6 * Math.PI / 3
        val x = Math.cos(v).toFloat
        val z = Math.sin(v).toFloat
        BlockVertexData(
          new Vector3f(x, (1 - a / 2).toFloat, z),
          new Vector2f((1 - a % 2).toFloat, (a / 2).toFloat),
          new Vector3f(nx, 0, nz),
          a
        )
      }
    }
  }
}

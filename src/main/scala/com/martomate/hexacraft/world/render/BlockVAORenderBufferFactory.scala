package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.renderer.{VAO, VAOBuilder, VBOBuilder}
import org.joml.{Vector2f, Vector3f}
import org.lwjgl.opengl.GL15

class BlockVAORenderBufferFactory(side: Int) extends RenderBufferFactory[VAORenderBuffer] {
  private val verticesPerInstance: Int = if (side < 2) 6 else 4

  override def bytesPerInstance: Int = (5 + verticesPerInstance) * 4

  override def makeBuffer(maxInstances: Int): VAORenderBuffer = new VAORenderBuffer(makeVAO(maxInstances), 1)

  def makeVAO(maxInstances: Int): VAO = new VAOBuilder(verticesPerInstance, maxInstances)
    .addVBO(VBOBuilder(verticesPerInstance, GL15.GL_STATIC_DRAW)
      .floats(0, 3)
      .floats(1, 2)
      .floats(2, 3)
      .ints(3, 1)
      .create().fill(0, setupBlockVBO(side)))
    .addVBO(VBOBuilder(maxInstances, GL15.GL_DYNAMIC_DRAW, 1)
      .ints(4, 3)
      .ints(5, 1)
      .floats(6, 1)
      .floatsArray(7, 1)(verticesPerInstance) // after this index should be 'this index' + verticesPerInstance
      .create())
    .create()

  protected def setupBlockVBO(s: Int): Seq[BlockVertexData] = {
    if (s < 2) {
      val ints = Seq(1, 2, 0, 3, 5, 4)

      for (i <- 0 until 6) yield {
        val v = {
          val a = ints(if (s == 0) i else 5 - i) * Math.PI / 3
          if (s == 0) -a else a
        }
        val x = Math.cos(v).toFloat
        val z = Math.sin(v).toFloat
        BlockVertexData(
          new Vector3f(x, 1 - s, z),
          new Vector2f((1 + (if (s == 0) -x else x)) / 2, (1 + z) / 2),
          new Vector3f(0, 1 - 2 * s, 0),
          i
        )
      }
    } else {
      val nv = ((s - 1) % 6 - 0.5) * Math.PI / 3
      val nx = Math.cos(nv).toFloat
      val nz = Math.sin(nv).toFloat

      for (i <- 0 until 4) yield {
        val v = (s - 2 + i % 2) % 6 * Math.PI / 3
        val x = Math.cos(v).toFloat
        val z = Math.sin(v).toFloat
        BlockVertexData(
          new Vector3f(x, 1 - i / 2, z),
          new Vector2f(1 - i % 2, i / 2),
          new Vector3f(nx, 0, nz),
          i
        )
      }
    }
  }
}

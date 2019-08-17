package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.renderer._
import org.joml.{Vector2f, Vector3f}
import org.lwjgl.opengl.{GL11, GL15}

class BlockRenderer(val side: Int, init_maxInstances: Int) {
  private var _maxInstances = init_maxInstances
  def maxInstances: Int = _maxInstances
  protected def verticesPerInstance: Int = if (side < 2) 6 else 4

  final val vao: VAO = initVAO()

  val renderer = new InstancedRenderer(vao, GL11.GL_TRIANGLE_STRIP)

  var instances = 0

  protected def initVAO(): VAO = new VAOBuilder(verticesPerInstance, maxInstances)
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
      .floatsArray(7, 1)(verticesPerInstance)// after this index should be 'this index' + verticesPerInstance
      .create())
    .create()

  def resize(newMaxInstances: Int): Unit = {
    _maxInstances = newMaxInstances
    vao.vbos(1).resize(newMaxInstances)
  }

  protected def setupBlockVBO(s: Int): Seq[BlockVertexData] = {
    if (s < 2) {
      val ints = Seq(1, 2, 0, 3, 5, 4)

      (0 until 6).map(i => {
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
      })
    } else {
      val nv = ((s - 1) % 6 - 0.5) * Math.PI / 3
      val nx = Math.cos(nv).toFloat
      val nz = Math.sin(nv).toFloat

      (0 until 4).map(i => {
        val v = (s - 2 + i % 2) % 6 * Math.PI / 3
        val x = Math.cos(v).toFloat
        val z = Math.sin(v).toFloat
        BlockVertexData(
          new Vector3f(x, 1 - i / 2, z),
          new Vector2f(1 - i % 2, i / 2),
          new Vector3f(nx, 0, nz),
          i
        )
      })
    }
  }

  def unload(): Unit = {
    vao.free()
  }
}

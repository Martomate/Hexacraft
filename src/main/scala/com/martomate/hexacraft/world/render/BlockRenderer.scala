package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.renderer.{InstancedRenderer, VAO, VAOBuilder, VBOBuilder}
import org.lwjgl.opengl.{GL11, GL15}

class EntityPartRenderer(_side: Int, _init_maxInstances: Int) extends BlockRenderer(_side, _init_maxInstances) {
  override val vao: VAO = new VAOBuilder(verticesPerInstance, maxInstances)
    .addVBO(VBOBuilder(verticesPerInstance, GL15.GL_STATIC_DRAW)
      .floats(0, 3)
      .floats(1, 2)
      .floats(2, 3)
      .create().fillFloats(0, setupBlockVBO(side)))
    .addVBO(VBOBuilder(maxInstances, GL15.GL_DYNAMIC_DRAW, 1)
      .floats(3, 4)
      .floats(4, 4)
      .floats(5, 4)
      .floats(6, 4)
      .ints(7, 1)
      .floats(8, 1)
      .create())
    .create()

  override val renderer = new InstancedRenderer(vao, GL11.GL_TRIANGLE_STRIP)
}

class BlockRenderer(val side: Int, init_maxInstances: Int) {
  private var _maxInstances = init_maxInstances
  def maxInstances: Int = _maxInstances
  protected def verticesPerInstance: Int = if (side < 2) 6 else 4

  val vao: VAO = new VAOBuilder(verticesPerInstance, maxInstances)
    .addVBO(VBOBuilder(verticesPerInstance, GL15.GL_STATIC_DRAW)
      .floats(0, 3)
      .floats(1, 2)
      .floats(2, 3)
      .create().fillFloats(0, setupBlockVBO(side)))
    .addVBO(VBOBuilder(maxInstances, GL15.GL_DYNAMIC_DRAW, 1)
      .ints(3, 3)
      .ints(4, 1)
      .floats(5, 1)
      .floats(6, 1)
      .create())
    .create()

  val renderer = new InstancedRenderer(vao, GL11.GL_TRIANGLE_STRIP)

  var instances = 0

  def resize(newMaxInstances: Int): Unit = {
    _maxInstances = newMaxInstances
    vao.vbos(1).resize(newMaxInstances)
  }

  protected def setupBlockVBO(s: Int): Seq[Float] = {
    if (s < 2) {
      val ints = Seq(1, 2, 0, 3, 5, 4)

      (0 until 6).flatMap(i => {
        val v = {
          val a = ints(if (s == 0) i else 5 - i) * Math.PI / 3
          if (s == 0) -a else a
        }
        val x = Math.cos(v).toFloat
        val z = Math.sin(v).toFloat
        Seq(x, 1 - s, z,
          (1 + (if (s == 0) -x else x)) / 2, (1 + z) / 2,
          0, 1 - 2 * s, 0)
      })
    } else {
      val nv = ((s - 1) % 6 - 0.5) * Math.PI / 3
      val nx = Math.cos(nv).toFloat
      val nz = Math.sin(nv).toFloat

      (0 until 4).flatMap(i => {
        val v = (s - 2 + i % 2) % 6 * Math.PI / 3
        val x = Math.cos(v).toFloat
        val z = Math.sin(v).toFloat
        Seq(x, 1 - i / 2, z,
          1 - i % 2, i / 2,
          nx, 0, nz)
      })
    }
  }

  def unload(): Unit = {
    vao.unload()
  }
}

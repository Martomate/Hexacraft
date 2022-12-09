package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.renderer._

import org.joml.{Vector2f, Vector3f}
import org.lwjgl.opengl.{GL11, GL15}

class BlockRenderer(val side: Int, init_maxInstances: Int):
  private var _maxInstances = init_maxInstances
  def maxInstances: Int = _maxInstances
  protected def verticesPerInstance: Int = if side < 2 then 6 else 4

  final val vao: VAO = initVAO()

  val renderer: Renderer = new InstancedRenderer(vao, GL11.GL_TRIANGLE_STRIP)

  var instances = 0

  protected def initVAO(): VAO =
    new VAOBuilder(verticesPerInstance, maxInstances)
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
        VBOBuilder(maxInstances, GL15.GL_DYNAMIC_DRAW, 1)
          .ints(4, 3)
          .ints(5, 1)
          .floats(6, 1)
          .floatsArray(7, 1)(
            verticesPerInstance
          ) // after this index should be 'this index' + verticesPerInstance
          .create()
      )
      .create()

  def resize(newMaxInstances: Int): Unit =
    _maxInstances = newMaxInstances
    vao.vbos(1).resize(newMaxInstances)

  protected def setupBlockVBO(s: Int): Seq[BlockVertexData] =
    if s < 2
    then
      val ints = Seq(1, 2, 0, 3, 5, 4)

      for i <- 0 until 6 yield
        val cornerIdx = if s == 0 then i else 5 - i
        val a = ints(cornerIdx) * Math.PI / 3
        val v = if s == 0 then -a else a

        val x = Math.cos(v).toFloat
        val z = Math.sin(v).toFloat

        val pos = new Vector3f(x, 1f - s, z)
        val tex = new Vector2f((1 + (if (s == 0) -x else x)) / 2, (1 + z) / 2)
        val norm = new Vector3f(0, 1f - 2f * s, 0)

        BlockVertexData(pos, tex, norm, i)
    else
      val nv = ((s - 1) % 6 - 0.5) * Math.PI / 3
      val nx = Math.cos(nv).toFloat
      val nz = Math.sin(nv).toFloat

      for i <- 0 until 4 yield
        val v = (s - 2 + i % 2) % 6 * Math.PI / 3
        val x = Math.cos(v).toFloat
        val z = Math.sin(v).toFloat

        val pos = new Vector3f(x, (1 - i / 2).toFloat, z)
        val tex = new Vector2f((1 - i % 2).toFloat, (i / 2).toFloat)
        val norm = new Vector3f(nx, 0, nz)

        BlockVertexData(pos, tex, norm, i)

  def unload(): Unit = vao.free()

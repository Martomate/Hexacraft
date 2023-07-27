package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.renderer.*
import org.joml.{Vector2f, Vector3f}

class BlockRenderer(val side: Int, val vao: VAO, val renderer: Renderer):
  private var _maxInstances = 0
  def maxInstances: Int = _maxInstances

  var instances = 0

  def resize(newMaxInstances: Int): Unit =
    _maxInstances = newMaxInstances
    vao.vbos(1).resize(newMaxInstances)

  def unload(): Unit = vao.free()

object BlockRenderer:
  private val topBottomVertexIndices =
    Seq(6, 0, 1, 6, 1, 2, 6, 2, 3, 6, 3, 4, 6, 4, 5, 6, 5, 0)

  private val topBottomTex =
    val l = new Vector2f(0, 0) // bottom left
    val t = new Vector2f(0.5f, 1) // top
    val r = new Vector2f(1, 0) // bottom right
    Seq(l, r, t, t, l, r, r, t, l, l, r, t, t, l, r, r, t, l)

  private val sideVertexIndices = Seq(0, 1, 3, 2, 0, 3)

  def verticesPerInstance(side: Int): Int = if side < 2 then 3 * 6 else 3 * 2

  def setupBlockVBO(s: Int): Seq[BlockVertexData] =
    if s < 2
    then setupBlockVboForTopOrBottom(s)
    else setupBlockVboForSide(s)

  private def setupBlockVboForTopOrBottom(s: Int): Seq[BlockVertexData] =
    val ints = topBottomVertexIndices
    val texCoords = topBottomTex

    for i <- 0 until verticesPerInstance(s) yield
      val cornerIdx = if s == 1 then i else verticesPerInstance(s) - 1 - i
      val a = ints(cornerIdx)
      val faceIndex = if s == 1 then i / 3 else (verticesPerInstance(s) - 1 - i) / 3

      val (x, z) =
        if a == 6 then (0f, 0f)
        else
          val v = a * Math.PI / 3
          (Math.cos(v).toFloat, Math.sin(v).toFloat)

      val pos = new Vector3f(x, 1f - s, z)
      val tex = texCoords(cornerIdx)
      val norm = new Vector3f(0, 1f - 2f * s, 0)

      BlockVertexData(pos, tex, norm, a, faceIndex)

  private def setupBlockVboForSide(s: Int): Seq[BlockVertexData] =
    val ints = sideVertexIndices

    val nv = ((s - 1) % 6 - 0.5) * Math.PI / 3
    val nx = Math.cos(nv).toFloat
    val nz = Math.sin(nv).toFloat

    for i <- 0 until verticesPerInstance(s) yield
      val a = ints(i)
      val v = (s - 2 + a % 2) % 6 * Math.PI / 3
      val x = Math.cos(v).toFloat
      val z = Math.sin(v).toFloat

      val pos = new Vector3f(x, (1 - a / 2).toFloat, z)
      val tex = new Vector2f((1 - a % 2).toFloat, (a / 2).toFloat)
      val norm = new Vector3f(nx, 0, nz)

      BlockVertexData(pos, tex, norm, a, 0)

package hexacraft.world.render

import hexacraft.infra.gpu.OpenGL
import hexacraft.renderer.{GpuState, InstancedRenderer, VAO, VertexData}

import org.joml.{Vector2f, Vector3f}
import org.lwjgl.BufferUtils

import java.nio.ByteBuffer

class BlockRenderer(vao: VAO, gpuState: GpuState) {
  private var usedInstances: Int = 0

  private val instanceVbo = vao.vbos(1)
  private val renderer: InstancedRenderer = InstancedRenderer(OpenGL.PrimitiveMode.Triangles, gpuState)

  def render(): Unit = {
    renderer.render(vao, usedInstances)
  }

  def setInstanceData(maxInstances: Int)(dataFiller: ByteBuffer => Unit): Unit = {
    val buf = BufferUtils.createByteBuffer(maxInstances * instanceVbo.stride)
    dataFiller(buf)

    val instances = buf.position() / instanceVbo.stride
    ensureCapacity(instances)
    usedInstances = instances

    buf.flip()
    instanceVbo.fill(0, buf)
  }

  private def ensureCapacity(instances: Int): Unit = {
    if instances > instanceVbo.capacity then {
      instanceVbo.resize((instances * 1.1f).toInt)
    }
  }

  def unload(): Unit = {
    vao.free()
  }
}

object BlockRenderer {
  case class BlockVertexData(
      position: Vector3f,
      texCoords: Vector2f,
      normal: Vector3f,
      vertexIndex: Int,
      faceIndex: Int
  ) extends VertexData {

    override def bytesPerVertex: Int = (3 + 2 + 3 + 1 + 1) * 4

    override def fill(buf: ByteBuffer): Unit = {
      buf.putFloat(position.x)
      buf.putFloat(position.y)
      buf.putFloat(position.z)

      buf.putFloat(texCoords.x)
      buf.putFloat(texCoords.y)

      buf.putFloat(normal.x)
      buf.putFloat(normal.y)
      buf.putFloat(normal.z)

      buf.putInt(vertexIndex)
      buf.putInt(faceIndex)
    }
  }

  private val topBottomVertexIndices =
    Seq(6, 0, 1, 6, 1, 2, 6, 2, 3, 6, 3, 4, 6, 4, 5, 6, 5, 0)

  private val topBottomTex =
    val l = new Vector2f(0, 0) // bottom left
    val t = new Vector2f(0.5f, 1) // top
    val r = new Vector2f(1, 0) // bottom right
    Seq(l, r, t, t, l, r, r, t, l, l, r, t, t, l, r, r, t, l)

  private val sideVertexIndices = Seq(0, 1, 3, 2, 0, 3)

  def verticesPerInstance(side: Int): Int = if side < 2 then 3 * 6 else 3 * 2

  def setupBlockVBO(s: Int): Seq[BlockVertexData] = {
    if s < 2 then {
      setupBlockVboForTopOrBottom(s)
    } else {
      setupBlockVboForSide(s)
    }
  }

  private def setupBlockVboForTopOrBottom(s: Int): Seq[BlockVertexData] = {
    val ints = topBottomVertexIndices
    val texCoords = topBottomTex

    for i <- 0 until verticesPerInstance(s) yield {
      val cornerIdx = i
      val a = ints(cornerIdx)
      val faceIndex = i / 3

      val (x, z) =
        if a == 6 then {
          (0f, 0f)
        } else {
          val v = a * Math.PI / 3
          var cos = Math.cos(v).toFloat
          val sin = Math.sin(v).toFloat
          if s == 0 then {
            cos = -cos
          }
          (cos, sin)
        }

      val pos = new Vector3f(x, 1f - s, z)
      val tex = texCoords(cornerIdx)
      val norm = new Vector3f(0, 1f - 2f * s, 0)

      BlockVertexData(pos, tex, norm, a, faceIndex)
    }
  }

  private def setupBlockVboForSide(s: Int): Seq[BlockVertexData] = {
    val ints = sideVertexIndices

    val nv = ((s - 1) % 6 - 0.5) * Math.PI / 3
    val nx = Math.cos(nv).toFloat
    val nz = Math.sin(nv).toFloat

    for i <- 0 until verticesPerInstance(s) yield {
      val a = ints(i)
      val v = (s - 2 + a % 2) % 6 * Math.PI / 3
      val x = Math.cos(v).toFloat
      val z = Math.sin(v).toFloat

      val pos = new Vector3f(x, (1 - a / 2).toFloat, z)
      val tex = new Vector2f((1 - a % 2).toFloat, (a / 2).toFloat)
      val norm = new Vector3f(nx, 0, nz)

      BlockVertexData(pos, tex, norm, a, 0)
    }
  }
}

package hexacraft.shaders

import hexacraft.infra.gpu.OpenGL.ShaderType.{Fragment, Vertex}
import hexacraft.infra.gpu.VboUsage
import hexacraft.renderer.{Shader, ShaderConfig, VAO, VertexData}

import org.joml.{Matrix4f, Vector2f, Vector3d, Vector3f}

import java.nio.ByteBuffer

class EntityShader(isSide: Boolean) {
  private val shader = Shader.from(
    ShaderConfig()
      .withStage(Vertex, "entity/vert.glsl")
      .withStage(Fragment, "entity/frag.glsl")
      .withInputs(
        "position",
        "texCoords",
        "normal",
        "vertexIndex",
        "faceIndex",
        "modelMatrix",
        "",
        "",
        "",
        "texOffset",
        "texDim",
        "blockTex",
        "brightness"
      )
      .withDefines("isSide" -> (if isSide then "1" else "0"))
  )

  def setTotalSize(totalSize: Int): Unit = {
    shader.setUniform1i("totalSize", totalSize)
  }

  def setCameraPosition(cam: Vector3d): Unit = {
    shader.setUniform3f("cam", cam.x.toFloat, cam.y.toFloat, cam.z.toFloat)
  }

  def setProjectionMatrix(matrix: Matrix4f): Unit = {
    shader.setUniformMat4("projMatrix", matrix)
  }

  def setViewMatrix(matrix: Matrix4f): Unit = {
    shader.setUniformMat4("viewMatrix", matrix)
  }

  def setSide(side: Int): Unit = {
    shader.setUniform1i("side", side)
  }

  def setTextureSize(texSize: Int): Unit = {
    shader.setUniform1i("texSize", texSize)
  }

  def enable(): Unit = {
    shader.activate()
  }

  def free(): Unit = {
    shader.free()
  }
}

object EntityShader {
  def createVao(side: Int): VAO = {
    VAO.build(verticesPerInstance(side))(
      _.addVertexVbo(verticesPerInstance(side), VboUsage.StaticDraw)(
        _.floats(0, 3)
          .floats(1, 2)
          .floats(2, 3)
          .ints(3, 1)
          .ints(4, 1),
        _.fill(0, setupBlockVBO(side))
      ).addInstanceVbo(0, VboUsage.DynamicDraw)(
        _.floatsArray(5, 4)(4)
          .ints(9, 2)
          .ints(10, 2)
          .ints(11, 1)
          .floats(12, 1)
      )
    )
  }

  def verticesPerInstance(side: Int): Int = if side < 2 then 3 * 6 else 3 * 2

  private val topBottomVertexIndices =
    Seq(6, 0, 1, 6, 1, 2, 6, 2, 3, 6, 3, 4, 6, 4, 5, 6, 5, 0)

  private val topBottomTex =
    val l = new Vector2f(0, 0) // bottom left
    val t = new Vector2f(0.5f, 1) // top
    val r = new Vector2f(1, 0) // bottom right
    Seq(l, r, t, t, l, r, r, t, l, l, r, t, t, l, r, r, t, l)

  private val sideVertexIndices = Seq(0, 1, 3, 2, 0, 3)

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

  class InstanceData(
      modelMatrix: Matrix4f,
      texOffset: (Int, Int),
      texSize: (Int, Int),
      blockTex: Int,
      brightness: Float
  ) {
    def fill(buf: ByteBuffer): Unit = {
      modelMatrix.get(buf)
      buf.position(buf.position() + 16 * 4)
      buf.putInt(texOffset._1)
      buf.putInt(texOffset._2)
      buf.putInt(texSize._1)
      buf.putInt(texSize._2)
      buf.putInt(blockTex)
      buf.putFloat(brightness)
    }
  }

}

package hexacraft.world.render

import hexacraft.renderer.VertexData
import hexacraft.world.entity.EntityModel

import org.joml.{Matrix4f, Vector2f, Vector3f}

import java.nio.ByteBuffer

case class EntityDataForShader(model: EntityModel, parts: Seq[EntityPartDataForShader])

case class EntityPartDataForShader(
    modelMatrix: Matrix4f,
    texOffset: (Int, Int),
    texSize: (Int, Int),
    blockTex: Int,
    brightness: Float
) {
  def fill(buf: ByteBuffer): Unit =
    modelMatrix.get(buf)
    buf.position(buf.position() + 16 * 4)
    buf.putInt(texOffset._1)
    buf.putInt(texOffset._2)
    buf.putInt(texSize._1)
    buf.putInt(texSize._2)
    buf.putInt(blockTex)
    buf.putFloat(brightness)
}

object ChunkRenderData {
  def blockSideStride(side: Int): Int = if (side < 2) (5 + 7) * 4 else (5 + 4) * 4

  def empty: ChunkRenderData = new ChunkRenderData(IndexedSeq.fill(8)(null))
}

case class ChunkRenderData(blockSide: IndexedSeq[ByteBuffer])

case class BlockVertexData(
    position: Vector3f,
    texCoords: Vector2f,
    normal: Vector3f,
    vertexIndex: Int,
    faceIndex: Int
) extends VertexData {

  override def bytesPerVertex: Int = (3 + 2 + 3 + 1 + 1) * 4

  override def fill(buf: ByteBuffer): Unit =
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

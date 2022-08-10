package com.martomate.hexacraft.world.render

import java.nio.ByteBuffer

import com.martomate.hexacraft.renderer.VertexData
import org.joml.{Vector2f, Vector3f}

case class BlockVertexData(
    position: Vector3f,
    texCoords: Vector2f,
    normal: Vector3f,
    vertexIndex: Int
) extends VertexData {

  override def bytesPerVertex: Int = (3 + 2 + 3 + 1) * 4

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
  }
}

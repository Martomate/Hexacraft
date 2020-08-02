package com.martomate.hexacraft.world.render

import java.nio.ByteBuffer

import org.joml.Matrix4f

case class EntityPartDataForShader(modelMatrix: Matrix4f,
                                   texOffset: (Int, Int),
                                   texSize: (Int, Int),
                                   blockTex: Int,
                                   brightness: Float) {
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

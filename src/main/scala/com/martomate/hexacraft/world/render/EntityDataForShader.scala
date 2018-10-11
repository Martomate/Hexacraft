package com.martomate.hexacraft.world.render

import java.nio.ByteBuffer

import org.joml.Matrix4f

case class EntityDataForShader(modelMatrix: Matrix4f, blockTex: Int, brightness: Float) {
  def fill(buf: ByteBuffer): Unit = {
    modelMatrix.get(buf)
    buf.position(buf.position() + 16 * 4)
    buf.putInt(blockTex)
    buf.putFloat(brightness)
  }
}

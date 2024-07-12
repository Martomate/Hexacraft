package hexacraft.client.render

import hexacraft.infra.gpu.OpenGL
import hexacraft.renderer.{GpuState, InstancedRenderer, VAO}

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

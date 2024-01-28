package hexacraft.renderer

import hexacraft.infra.gpu.OpenGL

class Renderer(mode: OpenGL.PrimitiveMode, gpuState: GpuState = GpuState()) {
  def render(vao: VAO): Unit = {
    val changedState = gpuState.set()
    vao.bind()
    OpenGL.glDrawArrays(mode, 0, vao.maxCount)
    changedState.unset()
  }
}

class InstancedRenderer(mode: OpenGL.PrimitiveMode, gpuState: GpuState = GpuState()) {
  def render(vao: VAO, primCount: Int): Unit = {
    val changedState = gpuState.set()
    vao.bind()
    OpenGL.glDrawArraysInstanced(mode, 0, vao.maxCount, primCount)
    changedState.unset()
  }
}

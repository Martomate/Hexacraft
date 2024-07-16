package hexacraft.renderer

import hexacraft.infra.gpu.OpenGL

class Renderer(mode: OpenGL.PrimitiveMode, gpuState: GpuState = GpuState()) {
  def render(vao: VAO, count: Int): Unit = {
    val changedState = gpuState.set()
    vao.bind()
    OpenGL.glDrawArrays(mode, 0, count)
    changedState.unset()
  }

  def render(vao: VAO, first: Int, count: Int): Unit = {
    val changedState = gpuState.set()
    vao.bind()
    OpenGL.glDrawArrays(mode, first, count)
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

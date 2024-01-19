package hexacraft.renderer

import hexacraft.infra.gpu.OpenGL

class Renderer(mode: OpenGL.PrimitiveMode, gpuState: GpuState = GpuState()) {
  def render(vao: VAO): Unit = {
    gpuState.set()
    vao.bind()
    OpenGL.glDrawArrays(mode, 0, vao.maxCount)
    gpuState.unset()
  }
}

class InstancedRenderer(mode: OpenGL.PrimitiveMode, gpuState: GpuState = GpuState()) {
  def render(vao: VAO, primCount: Int): Unit = {
    gpuState.set()
    vao.bind()
    OpenGL.glDrawArraysInstanced(mode, 0, vao.maxCount, primCount)
    gpuState.unset()
  }
}

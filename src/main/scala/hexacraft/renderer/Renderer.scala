package hexacraft.renderer

import hexacraft.infra.gpu.OpenGL

class Renderer(val vao: VAO, val mode: OpenGL.PrimitiveMode) {
  def render(): Unit = render(vao.maxCount)
  def render(count: Int): Unit = {
    vao.bind()
    OpenGL.glDrawArrays(mode, 0, count)
  }
}

class InstancedRenderer(_vao: VAO, _mode: OpenGL.PrimitiveMode) extends Renderer(_vao, _mode) {
  override def render(): Unit = render(vao.maxCount, vao.maxPrimCount)

  override def render(primCount: Int): Unit = render(vao.maxCount, primCount)

  def render(count: Int, primCount: Int): Unit = {
    vao.bind()
    OpenGL.glDrawArraysInstanced(mode, 0, count, primCount)
  }
}

trait NoDepthTest extends Renderer {
  override def render(): Unit = {
    OpenGL.glDisable(OpenGL.State.DepthTest)
    super.render()
    OpenGL.glEnable(OpenGL.State.DepthTest)
  }

  override def render(primCount: Int): Unit = {
    OpenGL.glDisable(OpenGL.State.DepthTest)
    super.render(primCount)
    OpenGL.glEnable(OpenGL.State.DepthTest)
  }
}

trait Blending extends Renderer {
  override def render(): Unit = {
    OpenGL.glEnable(OpenGL.State.Blend)
    super.render()
    OpenGL.glDisable(OpenGL.State.Blend)
  }

  override def render(primCount: Int): Unit = {
    OpenGL.glEnable(OpenGL.State.Blend)
    super.render(primCount)
    OpenGL.glDisable(OpenGL.State.Blend)
  }
}

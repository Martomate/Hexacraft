package hexagon.renderer

import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL31

class Renderer(val vao: VAO, val mode: Int) {
  def render(): Unit = render(vao.maxCount)
  def render(count: Int): Unit = {
    vao.bind()
    GL11.glDrawArrays(mode, 0, count)
  }
}

class InstancedRenderer(override val vao: VAO, override val mode: Int) extends Renderer(vao, mode) {
  override def render(): Unit = render(vao.maxCount, vao.maxPrimCount)

  override def render(primCount: Int): Unit = render(vao.maxCount, primCount)

  def render(count: Int, primCount: Int): Unit = {
    vao.bind()
    GL31.glDrawArraysInstanced(mode, 0, count, primCount);
  }
}

trait NoDepthTest extends Renderer {
  override def render(): Unit = {
    GL11.glDisable(GL11.GL_DEPTH_TEST)
    super.render()
    GL11.glEnable(GL11.GL_DEPTH_TEST)
  }
}

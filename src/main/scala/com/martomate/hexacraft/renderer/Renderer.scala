package com.martomate.hexacraft.renderer

import org.lwjgl.opengl.{GL11, GL31}

class Renderer(val vao: VAO, val mode: Int) {
  def render(): Unit = render(vao.maxCount)
  def render(count: Int): Unit = {
    vao.bind()
    GL11.glDrawArrays(mode, 0, count)
  }
}

class InstancedRenderer(_vao: VAO, _mode: Int) extends Renderer(_vao, _mode) {
  override def render(): Unit = render(vao.maxCount, vao.maxPrimCount)

  override def render(primCount: Int): Unit = render(vao.maxCount, primCount)

  def render(count: Int, primCount: Int): Unit = {
    vao.bind()
    GL31.glDrawArraysInstanced(mode, 0, count, primCount)
  }
}

trait NoDepthTest extends Renderer {
  override def render(): Unit = {
    GL11.glDisable(GL11.GL_DEPTH_TEST)
    super.render()
    GL11.glEnable(GL11.GL_DEPTH_TEST)
  }

  override def render(primCount: Int): Unit = {
    GL11.glDisable(GL11.GL_DEPTH_TEST)
    super.render(primCount)
    GL11.glEnable(GL11.GL_DEPTH_TEST)
  }
}

trait Blending extends Renderer {
  override def render(): Unit = {
    GL11.glEnable(GL11.GL_BLEND)
    super.render()
    GL11.glDisable(GL11.GL_BLEND)
  }

  override def render(primCount: Int): Unit = {
    GL11.glEnable(GL11.GL_BLEND)
    super.render(primCount)
    GL11.glDisable(GL11.GL_BLEND)
  }
}

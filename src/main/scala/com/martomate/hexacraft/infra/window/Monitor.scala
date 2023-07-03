package com.martomate.hexacraft.infra.window

import com.martomate.hexacraft.util.PointerWrapper

class Monitor(val id: Monitor.Id, glfw: GlfwWrapper) {
  private val pointerWrapper = new PointerWrapper()

  def position: (Int, Int) =
    pointerWrapper.ints((px, py) => glfw.glfwGetMonitorPos(id.toLong, px, py))

  def videoMode: VideoMode = glfw.glfwGetVideoMode(id.toLong)
}

object Monitor {
  opaque type Id <: AnyVal = Long
  object Id {
    def apply(id: Long): Id = id
    extension (id: Id) def toLong: Long = id
  }
}

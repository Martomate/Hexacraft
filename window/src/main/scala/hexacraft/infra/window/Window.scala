package hexacraft.infra.window

import hexacraft.util.PointerWrapper

import org.lwjgl.glfw.GLFW

object Window {
  opaque type Id <: AnyVal = Long
  object Id {
    def apply(id: Long): Id = id
    extension (id: Id) def toLong: Long = id
  }
}

class Window(val id: Window.Id, glfw: GlfwWrapper) {
  private val pointerWrapper = new PointerWrapper()

  def position: (Int, Int) = pointerWrapper.ints((px, py) => glfw.glfwGetWindowPos(id.toLong, px, py))

  def size: (Int, Int) = pointerWrapper.ints((px, py) => glfw.glfwGetWindowSize(id.toLong, px, py))

  def framebufferSize: (Int, Int) = pointerWrapper.ints((px, py) => glfw.glfwGetFramebufferSize(id.toLong, px, py))

  def cursorPosition: (Double, Double) = pointerWrapper.doubles((px, py) => glfw.glfwGetCursorPos(id.toLong, px, py))

  def shouldClose: Boolean = glfw.glfwWindowShouldClose(id.toLong)

  def requestClose(): Unit = glfw.glfwSetWindowShouldClose(id.toLong, true)

  def close(): Unit = {
    glfw.glfwFreeCallbacks(id.toLong)
    glfw.glfwDestroyWindow(id.toLong)
  }

  def makeContextCurrent(): Unit = glfw.glfwMakeContextCurrent(id.toLong)

  def show(): Unit = glfw.glfwShowWindow(id.toLong)

  def moveTo(x: Int, y: Int): Unit = glfw.glfwSetWindowPos(id.toLong, x, y)

  def setTitle(title: String): Unit = glfw.glfwSetWindowTitle(id.toLong, title)

  def isKeyPressed(key: KeyboardKey): Boolean = glfw.glfwGetKey(id.toLong, key.toGlfw) == GLFW.GLFW_PRESS

  def setCursorMode(mode: CursorMode): Unit = {
    glfw.glfwSetInputMode(id.toLong, GLFW.GLFW_CURSOR, mode.toGlfw)
  }

  def swapBuffers(): Unit = glfw.glfwSwapBuffers(id.toLong)

  def enterFullscreenMode(monitor: Monitor): Unit = {
    val mode = monitor.videoMode
    glfw.glfwSetWindowMonitor(id.toLong, monitor.id.toLong, 0, 0, mode.width, mode.height, mode.refreshRate)
  }

  def enterWindowedMode(x: Int, y: Int, width: Int, height: Int): Unit = {
    glfw.glfwSetWindowMonitor(id.toLong, 0, x, y, width, height, GLFW.GLFW_DONT_CARE)
  }

  def setKeyCallback(callback: CallbackEvent.KeyPressed => Unit): Unit = {
    glfw.glfwSetKeyCallback(
      id.toLong,
      (_, key, scancode, action, mods) =>
        callback(
          CallbackEvent.KeyPressed(
            this,
            KeyboardKey.fromGlfw(key),
            scancode,
            KeyAction.fromGlfw(action),
            KeyMods.fromGlfw(mods)
          )
        )
    )
  }

  def setCharCallback(callback: CallbackEvent.CharTyped => Unit): Unit = {
    glfw.glfwSetCharCallback(
      id.toLong,
      (_, character) => callback(CallbackEvent.CharTyped(this, character))
    )
  }

  def setMouseButtonCallback(callback: CallbackEvent.MouseClicked => Unit): Unit = {
    glfw.glfwSetMouseButtonCallback(
      id.toLong,
      (_, button, action, mods) =>
        callback(
          CallbackEvent.MouseClicked(
            this,
            MouseButton.fromGlfw(button),
            MouseAction.fromGlfw(action),
            KeyMods.fromGlfw(mods)
          )
        )
    )
  }

  def setWindowSizeCallback(callback: CallbackEvent.WindowResized => Unit): Unit = {
    glfw.glfwSetWindowSizeCallback(
      id.toLong,
      (_, width, height) => callback(CallbackEvent.WindowResized(this, width, height))
    )
  }

  def setWindowFocusCallback(callback: CallbackEvent.WindowFocusChanged => Unit): Unit = {
    glfw.glfwSetWindowFocusCallback(
      id.toLong,
      (_, focused) => callback(CallbackEvent.WindowFocusChanged(this, focused))
    )
  }

  def setFrameBufferSizeCallback(callback: CallbackEvent.FrameBufferResized => Unit): Unit = {
    glfw.glfwSetFramebufferSizeCallback(
      id.toLong,
      (_, width, height) => callback(CallbackEvent.FrameBufferResized(this, width, height))
    )
  }

  def setScrollCallback(callback: CallbackEvent.MouseScrolled => Unit): Unit = {
    glfw.glfwSetScrollCallback(
      id.toLong,
      (_, dx, dy) => callback(CallbackEvent.MouseScrolled(this, dx, dy))
    )
  }
}

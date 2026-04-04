package hexacraft.infra.window

import hexacraft.util.PointerWrapper

import org.lwjgl.glfw.GLFW

import scala.collection.mutable

object Window {
  opaque type Id <: AnyVal = Long
  object Id {
    def apply(id: Long): Id = id
    extension (id: Id) def toLong: Long = id
  }
}

class Window(val id: Window.Id, glfw: GlfwWrapper) {
  private val pointerWrapper = new PointerWrapper()
  private val eventQueue = mutable.Queue.empty[CallbackEvent]

  def position: (Int, Int) = pointerWrapper.synchronized {
    pointerWrapper.ints((px, py) => glfw.glfwGetWindowPos(id.toLong, px, py))
  }

  def size: (Int, Int) = pointerWrapper.synchronized {
    pointerWrapper.ints((px, py) => glfw.glfwGetWindowSize(id.toLong, px, py))
  }

  def framebufferSize: (Int, Int) = pointerWrapper.synchronized {
    pointerWrapper.ints((px, py) => glfw.glfwGetFramebufferSize(id.toLong, px, py))
  }

  def cursorPosition: (Double, Double) = pointerWrapper.synchronized {
    pointerWrapper.doubles((px, py) => glfw.glfwGetCursorPos(id.toLong, px, py))
  }

  def shouldClose: Boolean = glfw.glfwWindowShouldClose(id.toLong)

  def requestClose(): Unit = glfw.glfwSetWindowShouldClose(id.toLong, true)

  def close(): Unit = {
    glfw.glfwFreeCallbacks(id.toLong)
    glfw.glfwDestroyWindow(id.toLong)
  }

  def activateContext(): Unit = glfw.glfwMakeContextCurrent(id.toLong)
  def deactivateContext(): Unit = glfw.glfwMakeContextCurrent(0)

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

  private[window] def addEvent(event: CallbackEvent): Unit = {
    eventQueue.synchronized {
      eventQueue.enqueue(event)
    }
  }

  def nextEvent: Option[CallbackEvent] = {
    eventQueue.synchronized {
      if eventQueue.nonEmpty then Some(eventQueue.dequeue) else None
    }
  }
}

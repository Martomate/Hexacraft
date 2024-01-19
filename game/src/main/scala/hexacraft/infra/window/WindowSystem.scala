package hexacraft.infra.window

import hexacraft.util.PointerWrapper

import org.lwjgl.PointerBuffer
import org.lwjgl.glfw.*
import org.lwjgl.system.MemoryUtil

import scala.collection.mutable

object WindowSystem {
  def create(): WindowSystem = new WindowSystem(RealGlfw)
  def createNull(): WindowSystem = new WindowSystem(new NullGlfw)
}

class WindowSystem(glfw: GlfwWrapper) {
  def shutdown(): Unit = {
    glfw.glfwTerminate()
    glfw.glfwSetErrorCallback(null).free()
  }

  /** Initialize GLFW. Most GLFW functions will not work before doing this. */
  def initialize(): Unit = {
    if !glfw.glfwInit()
    then throw new IllegalStateException("Unable to initialize GLFW")
  }

  def setErrorCallback(callback: ErrorEvent => Unit): Unit = {
    glfw.glfwSetErrorCallback((error, descriptionPointer) => {
      val description = MemoryUtil.memUTF8(descriptionPointer)
      val reason = WindowErrorReason.fromGlfw(error)
      callback(ErrorEvent(reason, description))
    })
  }

  /** Poll for window events. The callbacks will (on most systems) only be invoked during this call. */
  def runEventCallbacks(): Unit = {
    glfw.glfwPollEvents()
  }

  /** Enables/disables vsync for the current context */
  def setVsync(enabled: Boolean): Unit = {
    glfw.glfwSwapInterval(if enabled then 1 else 0)
  }

  def primaryMonitor: Monitor = {
    new Monitor(Monitor.Id(glfw.glfwGetPrimaryMonitor()), glfw)
  }

  def monitors: Seq[Monitor] = {
    val res = mutable.ArrayBuffer.empty[Monitor]

    val monitors = glfw.glfwGetMonitors()
    while monitors.hasRemaining do {
      res += new Monitor(Monitor.Id(monitors.get), glfw)
    }

    res.toSeq
  }

  def createWindow(settings: WindowSettings): Option[Window] = {
    val glfwResizable = if settings.resizable then GLFW.GLFW_TRUE else GLFW.GLFW_FALSE
    val glfwDebugMode = if settings.opengl.debugMode then GLFW.GLFW_TRUE else GLFW.GLFW_FALSE

    // Configure the window
    glfw.glfwDefaultWindowHints()
    glfw.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE)
    glfw.glfwWindowHint(GLFW.GLFW_RESIZABLE, glfwResizable)
    glfw.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, settings.opengl.majorVersion)
    glfw.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, settings.opengl.minorVersion)
    glfw.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE)
    glfw.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE)
    glfw.glfwWindowHint(GLFW.GLFW_OPENGL_DEBUG_CONTEXT, glfwDebugMode)
    glfw.glfwWindowHint(GLFW.GLFW_SAMPLES, settings.samples)

    // Create the window
    val id = glfw.glfwCreateWindow(settings.width, settings.height, settings.title, 0, 0)
    if id != 0 then {
      Some(new Window(Window.Id(id), glfw))
    } else {
      None
    }
  }
}

case class ErrorEvent(reason: WindowErrorReason, description: String)

case class VideoMode(width: Int, height: Int, redBits: Int, greenBits: Int, blueBits: Int, refreshRate: Int)

trait GlfwWrapper {
  def glfwGetWindowPos(window: Long, xpos: Array[Int], ypos: Array[Int]): Unit
  def glfwGetWindowSize(window: Long, xpos: Array[Int], ypos: Array[Int]): Unit
  def glfwGetFramebufferSize(window: Long, xpos: Array[Int], ypos: Array[Int]): Unit
  def glfwGetMonitorPos(monitor: Long, xpos: Array[Int], ypos: Array[Int]): Unit
  def glfwGetCursorPos(window: Long, xpos: Array[Double], ypos: Array[Double]): Unit
  def glfwGetMonitors(): PointerBuffer
  def glfwGetPrimaryMonitor(): Long
  def glfwGetVideoMode(monitor: Long): VideoMode
  def glfwSetWindowMonitor(
      window: Long,
      monitor: Long,
      xpos: Int,
      ypos: Int,
      width: Int,
      height: Int,
      refreshRate: Int
  ): Unit
  def glfwSetKeyCallback(window: Long, callback: GLFWKeyCallbackI): Unit
  def glfwSetCharCallback(window: Long, callback: GLFWCharCallbackI): Unit
  def glfwSetMouseButtonCallback(window: Long, callback: GLFWMouseButtonCallbackI): Unit
  def glfwSetWindowSizeCallback(window: Long, callback: GLFWWindowSizeCallbackI): Unit
  def glfwSetFramebufferSizeCallback(window: Long, callback: GLFWFramebufferSizeCallbackI): Unit
  def glfwSetScrollCallback(window: Long, callback: GLFWScrollCallbackI): Unit
  def glfwGetKey(window: Long, key: Int): Int
  def glfwSetInputMode(window: Long, mode: Int, value: Int): Unit
  def glfwWindowShouldClose(window: Long): Boolean
  def glfwSwapBuffers(window: Long): Unit
  def glfwSetWindowTitle(window: Long, title: String): Unit
  def glfwPollEvents(): Unit
  def glfwSwapInterval(interval: Int): Unit
  def glfwDestroyWindow(window: Long): Unit
  def glfwTerminate(): Unit
  def glfwSetErrorCallback(callback: GLFWErrorCallbackI): GLFWErrorCallback
  def glfwInit(): Boolean
  def glfwMakeContextCurrent(window: Long): Unit
  def glfwShowWindow(window: Long): Unit
  def glfwCreateWindow(width: Int, height: Int, title: String, monitor: Long, share: Long): Long
  def glfwDefaultWindowHints(): Unit
  def glfwWindowHint(hint: Int, value: Int): Unit
  def glfwSetWindowShouldClose(window: Long, value: Boolean): Unit
  def glfwSetWindowPos(window: Long, xpos: Int, ypos: Int): Unit
  def glfwFreeCallbacks(window: Long): Unit
}

object RealGlfw extends GlfwWrapper {
  def glfwGetWindowPos(window: Long, xpos: Array[Int], ypos: Array[Int]): Unit = {
    GLFW.glfwGetWindowPos(window, xpos, ypos)
  }

  def glfwGetWindowSize(window: Long, xpos: Array[Int], ypos: Array[Int]): Unit = {
    GLFW.glfwGetWindowSize(window, xpos, ypos)
  }

  def glfwGetFramebufferSize(window: Long, xpos: Array[Int], ypos: Array[Int]): Unit = {
    GLFW.glfwGetFramebufferSize(window, xpos, ypos)
  }

  def glfwGetMonitorPos(monitor: Long, xpos: Array[Int], ypos: Array[Int]): Unit = {
    GLFW.glfwGetMonitorPos(monitor, xpos, ypos)
  }

  def glfwGetCursorPos(window: Long, xpos: Array[Double], ypos: Array[Double]): Unit = {
    GLFW.glfwGetCursorPos(window, xpos, ypos)
  }

  def glfwGetMonitors(): PointerBuffer = {
    GLFW.glfwGetMonitors()
  }

  def glfwGetPrimaryMonitor(): Long = {
    GLFW.glfwGetPrimaryMonitor()
  }

  def glfwGetVideoMode(monitor: Long): VideoMode = {
    val mode = GLFW.glfwGetVideoMode(monitor)
    VideoMode(mode.width, mode.height, mode.redBits, mode.greenBits, mode.blueBits, mode.refreshRate)
  }

  def glfwSetWindowMonitor(
      window: Long,
      monitor: Long,
      xpos: Int,
      ypos: Int,
      width: Int,
      height: Int,
      refreshRate: Int
  ): Unit = {
    GLFW.glfwSetWindowMonitor(window, monitor, xpos, ypos, width, height, refreshRate)
  }

  def glfwSetKeyCallback(window: Long, callback: GLFWKeyCallbackI): Unit = {
    GLFW.glfwSetKeyCallback(window, callback)
  }

  def glfwSetCharCallback(window: Long, callback: GLFWCharCallbackI): Unit = {
    GLFW.glfwSetCharCallback(window, callback)
  }

  def glfwSetMouseButtonCallback(window: Long, callback: GLFWMouseButtonCallbackI): Unit = {
    GLFW.glfwSetMouseButtonCallback(window, callback)
  }

  def glfwSetWindowSizeCallback(window: Long, callback: GLFWWindowSizeCallbackI): Unit = {
    GLFW.glfwSetWindowSizeCallback(window, callback)
  }

  def glfwSetFramebufferSizeCallback(window: Long, callback: GLFWFramebufferSizeCallbackI): Unit = {
    GLFW.glfwSetFramebufferSizeCallback(window, callback)
  }

  def glfwSetScrollCallback(window: Long, callback: GLFWScrollCallbackI): Unit = {
    GLFW.glfwSetScrollCallback(window, callback)
  }

  def glfwGetKey(window: Long, key: Int): Int = {
    GLFW.glfwGetKey(window, key)
  }

  def glfwSetInputMode(window: Long, mode: Int, value: Int): Unit = {
    GLFW.glfwSetInputMode(window, mode, value)
  }

  def glfwWindowShouldClose(window: Long): Boolean = {
    GLFW.glfwWindowShouldClose(window)
  }

  def glfwSwapBuffers(window: Long): Unit = {
    GLFW.glfwSwapBuffers(window)
  }

  def glfwSetWindowTitle(window: Long, title: String): Unit = {
    GLFW.glfwSetWindowTitle(window, title)
  }

  def glfwPollEvents(): Unit = {
    GLFW.glfwPollEvents()
  }

  def glfwSwapInterval(interval: Int): Unit = {
    GLFW.glfwSwapInterval(interval)
  }

  def glfwDestroyWindow(window: Long): Unit = {
    GLFW.glfwDestroyWindow(window)
  }

  def glfwTerminate(): Unit = {
    GLFW.glfwTerminate()
  }

  def glfwSetErrorCallback(callback: GLFWErrorCallbackI): GLFWErrorCallback = {
    GLFW.glfwSetErrorCallback(callback)
  }

  def glfwInit(): Boolean = {
    GLFW.glfwInit()
  }

  def glfwMakeContextCurrent(window: Long): Unit = {
    GLFW.glfwMakeContextCurrent(window)
  }

  def glfwShowWindow(window: Long): Unit = {
    GLFW.glfwShowWindow(window)
  }

  def glfwCreateWindow(
      width: Int,
      height: Int,
      title: String,
      monitor: Long,
      share: Long
  ): Long = {
    GLFW.glfwCreateWindow(width, height, title, monitor, share)
  }

  def glfwDefaultWindowHints(): Unit = {
    GLFW.glfwDefaultWindowHints()
  }

  def glfwWindowHint(hint: Int, value: Int): Unit = {
    GLFW.glfwWindowHint(hint, value)
  }

  def glfwSetWindowShouldClose(window: Long, value: Boolean): Unit = {
    GLFW.glfwSetWindowShouldClose(window, value)
  }

  def glfwSetWindowPos(window: Long, xpos: Int, ypos: Int): Unit = {
    GLFW.glfwSetWindowPos(window, xpos, ypos)
  }

  def glfwFreeCallbacks(window: Long): Unit = {
    Callbacks.glfwFreeCallbacks(window)
  }
}

class NullGlfw extends GlfwWrapper {
  def glfwGetWindowPos(window: Long, xpos: Array[Int], ypos: Array[Int]): Unit = ()
  def glfwGetWindowSize(window: Long, xpos: Array[Int], ypos: Array[Int]): Unit = ()
  def glfwGetFramebufferSize(window: Long, xpos: Array[Int], ypos: Array[Int]): Unit = ()
  def glfwGetMonitorPos(monitor: Long, xpos: Array[Int], ypos: Array[Int]): Unit = ()
  def glfwGetCursorPos(window: Long, xpos: Array[Double], ypos: Array[Double]): Unit = ()
  def glfwGetMonitors(): PointerBuffer = PointerBuffer.allocateDirect(0)
  def glfwGetPrimaryMonitor(): Long = 123
  def glfwGetVideoMode(monitor: Long): VideoMode = null
  def glfwSetWindowMonitor(
      window: Long,
      monitor: Long,
      xpos: Int,
      ypos: Int,
      width: Int,
      height: Int,
      refreshRate: Int
  ): Unit = ()
  def glfwSetKeyCallback(window: Long, callback: GLFWKeyCallbackI): Unit = ()
  def glfwSetCharCallback(window: Long, callback: GLFWCharCallbackI): Unit = ()
  def glfwSetMouseButtonCallback(window: Long, callback: GLFWMouseButtonCallbackI): Unit = ()
  def glfwSetWindowSizeCallback(window: Long, callback: GLFWWindowSizeCallbackI): Unit = ()
  def glfwSetFramebufferSizeCallback(window: Long, callback: GLFWFramebufferSizeCallbackI): Unit = ()
  def glfwSetScrollCallback(window: Long, callback: GLFWScrollCallbackI): Unit = ()
  def glfwGetKey(window: Long, key: Int): Int = GLFW.GLFW_RELEASE
  def glfwSetInputMode(window: Long, mode: Int, value: Int): Unit = ()
  def glfwWindowShouldClose(window: Long): Boolean = false
  def glfwSwapBuffers(window: Long): Unit = ()
  def glfwSetWindowTitle(window: Long, title: String): Unit = ()
  def glfwPollEvents(): Unit = ()
  def glfwSwapInterval(interval: Int): Unit = ()
  def glfwDestroyWindow(window: Long): Unit = ()
  def glfwTerminate(): Unit = ()
  def glfwSetErrorCallback(callback: GLFWErrorCallbackI): GLFWErrorCallback = null
  def glfwInit(): Boolean = true
  def glfwMakeContextCurrent(window: Long): Unit = ()
  def glfwShowWindow(window: Long): Unit = ()
  def glfwCreateWindow(width: Int, height: Int, title: String, monitor: Long, share: Long): Long = 234
  def glfwDefaultWindowHints(): Unit = ()
  def glfwWindowHint(hint: Int, value: Int): Unit = ()
  def glfwSetWindowShouldClose(window: Long, value: Boolean): Unit = ()
  def glfwSetWindowPos(window: Long, xpos: Int, ypos: Int): Unit = ()
  def glfwFreeCallbacks(window: Long): Unit = ()
}

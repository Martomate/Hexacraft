package hexacraft.infra.window

import hexacraft.util.{EventDispatcher, Tracker}

import org.lwjgl.PointerBuffer
import org.lwjgl.glfw.*
import org.lwjgl.system.MemoryUtil

import scala.collection.mutable
import scala.concurrent.{Await, Promise}
import scala.concurrent.duration.Duration

object WindowSystem {
  def create(): WindowSystem = new WindowSystem(RealGlfw)
  def createNull(config: NullConfig = NullConfig()): WindowSystem = new WindowSystem(new NullGlfw(config))

  case class NullConfig(
      shouldClose: Boolean = false
  )

  enum Event {
    case Initialized
    case WindowCreated(width: Int, height: Int)
  }
}

class WindowSystem(glfw: GlfwWrapper) {
  private val dispatcher = new EventDispatcher[WindowSystem.Event]
  def trackEvents(tracker: Tracker[WindowSystem.Event]): Unit = dispatcher.track(tracker)

  def shutdown(): Unit = {
    glfw.glfwTerminate()
    glfw.glfwSetErrorCallback(null).free()
  }

  /** Initialize GLFW. Most GLFW functions will not work before doing this. */
  def initialize(): Unit = {
    if !glfw.glfwInit()
    then throw new IllegalStateException("Unable to initialize GLFW")

    dispatcher.notify(WindowSystem.Event.Initialized)
  }

  def performCallsAsMainThread(): Unit = {
    glfw.performCallsAsMainThread()
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
    glfw.glfwWindowHint(GLFW.GLFW_AUTO_ICONIFY, GLFW.GLFW_FALSE)
    glfw.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, settings.opengl.majorVersion)
    glfw.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, settings.opengl.minorVersion)
    glfw.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE)
    glfw.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE)
    glfw.glfwWindowHint(GLFW.GLFW_OPENGL_DEBUG_CONTEXT, glfwDebugMode)
    glfw.glfwWindowHint(GLFW.GLFW_SAMPLES, settings.samples)

    // Create the window
    val id = glfw.glfwCreateWindow(settings.width, settings.height, settings.title, 0, 0)
    if id != 0 then {
      dispatcher.notify(WindowSystem.Event.WindowCreated(settings.width, settings.height))

      Some(new Window(Window.Id(id), glfw))
    } else {
      None
    }
  }
}

case class ErrorEvent(reason: WindowErrorReason, description: String)

case class VideoMode(width: Int, height: Int, redBits: Int, greenBits: Int, blueBits: Int, refreshRate: Int)

trait GlfwWrapper {
  def performCallsAsMainThread(): Unit

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
  def glfwSetCursorPosCallback(window: Long, callback: GLFWCursorPosCallbackI): Unit
  def glfwSetWindowSizeCallback(window: Long, callback: GLFWWindowSizeCallbackI): Unit
  def glfwSetWindowFocusCallback(window: Long, callback: GLFWWindowFocusCallbackI): Unit
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
  private val calls = mutable.Queue.empty[(() => Any, Promise[Any])]
  private def runOnMainThread[A](r: => A): A = {
    val promise = Promise[Any]()
    calls.synchronized {
      calls.enqueue((() => r, promise))
    }
    Await.result(promise.future, Duration.Inf).asInstanceOf[A]
  }

  def performCallsAsMainThread(): Unit = {
    while calls.nonEmpty do {
      val (r, p) = calls.synchronized { calls.dequeue() }
      p.success(r())
    }
  }

  def glfwGetWindowPos(window: Long, xpos: Array[Int], ypos: Array[Int]): Unit = {
    runOnMainThread(GLFW.glfwGetWindowPos(window, xpos, ypos))
  }

  def glfwGetWindowSize(window: Long, xpos: Array[Int], ypos: Array[Int]): Unit = {
    runOnMainThread(GLFW.glfwGetWindowSize(window, xpos, ypos))
  }

  def glfwGetFramebufferSize(window: Long, xpos: Array[Int], ypos: Array[Int]): Unit = {
    runOnMainThread(GLFW.glfwGetFramebufferSize(window, xpos, ypos))
  }

  def glfwGetMonitorPos(monitor: Long, xpos: Array[Int], ypos: Array[Int]): Unit = {
    runOnMainThread(GLFW.glfwGetMonitorPos(monitor, xpos, ypos))
  }

  def glfwGetCursorPos(window: Long, xpos: Array[Double], ypos: Array[Double]): Unit = {
    runOnMainThread(GLFW.glfwGetCursorPos(window, xpos, ypos))
  }

  def glfwGetMonitors(): PointerBuffer = {
    runOnMainThread(GLFW.glfwGetMonitors())
  }

  def glfwGetPrimaryMonitor(): Long = {
    runOnMainThread(GLFW.glfwGetPrimaryMonitor())
  }

  def glfwGetVideoMode(monitor: Long): VideoMode = runOnMainThread {
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
    runOnMainThread(GLFW.glfwSetWindowMonitor(window, monitor, xpos, ypos, width, height, refreshRate))
  }

  def glfwSetKeyCallback(window: Long, callback: GLFWKeyCallbackI): Unit = {
    runOnMainThread(GLFW.glfwSetKeyCallback(window, callback))
  }

  def glfwSetCharCallback(window: Long, callback: GLFWCharCallbackI): Unit = {
    runOnMainThread(GLFW.glfwSetCharCallback(window, callback))
  }

  def glfwSetMouseButtonCallback(window: Long, callback: GLFWMouseButtonCallbackI): Unit = {
    runOnMainThread(GLFW.glfwSetMouseButtonCallback(window, callback))
  }

  def glfwSetCursorPosCallback(window: Long, callback: GLFWCursorPosCallbackI): Unit = {
    runOnMainThread(GLFW.glfwSetCursorPosCallback(window, callback))
  }

  def glfwSetWindowSizeCallback(window: Long, callback: GLFWWindowSizeCallbackI): Unit = {
    runOnMainThread(GLFW.glfwSetWindowSizeCallback(window, callback))
  }

  def glfwSetWindowFocusCallback(window: Long, callback: GLFWWindowFocusCallbackI): Unit = {
    runOnMainThread(GLFW.glfwSetWindowFocusCallback(window, callback))
  }

  def glfwSetFramebufferSizeCallback(window: Long, callback: GLFWFramebufferSizeCallbackI): Unit = {
    runOnMainThread(GLFW.glfwSetFramebufferSizeCallback(window, callback))
  }

  def glfwSetScrollCallback(window: Long, callback: GLFWScrollCallbackI): Unit = {
    runOnMainThread(GLFW.glfwSetScrollCallback(window, callback))
  }

  def glfwGetKey(window: Long, key: Int): Int = {
    runOnMainThread(GLFW.glfwGetKey(window, key))
  }

  def glfwSetInputMode(window: Long, mode: Int, value: Int): Unit = {
    runOnMainThread(GLFW.glfwSetInputMode(window, mode, value))
  }

  def glfwWindowShouldClose(window: Long): Boolean = {
    GLFW.glfwWindowShouldClose(window)
  }

  def glfwSwapBuffers(window: Long): Unit = {
    GLFW.glfwSwapBuffers(window)
  }

  def glfwSetWindowTitle(window: Long, title: String): Unit = {
    runOnMainThread(GLFW.glfwSetWindowTitle(window, title))
  }

  def glfwPollEvents(): Unit = {
    runOnMainThread(GLFW.glfwPollEvents())
  }

  def glfwSwapInterval(interval: Int): Unit = {
    GLFW.glfwSwapInterval(interval)
  }

  def glfwDestroyWindow(window: Long): Unit = {
    runOnMainThread(GLFW.glfwDestroyWindow(window))
  }

  def glfwTerminate(): Unit = {
    runOnMainThread(GLFW.glfwTerminate())
  }

  def glfwSetErrorCallback(callback: GLFWErrorCallbackI): GLFWErrorCallback = {
    runOnMainThread(GLFW.glfwSetErrorCallback(callback))
  }

  def glfwInit(): Boolean = {
    runOnMainThread(GLFW.glfwInit())
  }

  def glfwMakeContextCurrent(window: Long): Unit = {
    GLFW.glfwMakeContextCurrent(window)
  }

  def glfwShowWindow(window: Long): Unit = {
    runOnMainThread(GLFW.glfwShowWindow(window))
  }

  def glfwCreateWindow(
      width: Int,
      height: Int,
      title: String,
      monitor: Long,
      share: Long
  ): Long = {
    runOnMainThread(GLFW.glfwCreateWindow(width, height, title, monitor, share))
  }

  def glfwDefaultWindowHints(): Unit = {
    runOnMainThread(GLFW.glfwDefaultWindowHints())
  }

  def glfwWindowHint(hint: Int, value: Int): Unit = {
    runOnMainThread(GLFW.glfwWindowHint(hint, value))
  }

  def glfwSetWindowShouldClose(window: Long, value: Boolean): Unit = {
    GLFW.glfwSetWindowShouldClose(window, value)
  }

  def glfwSetWindowPos(window: Long, xpos: Int, ypos: Int): Unit = {
    runOnMainThread(GLFW.glfwSetWindowPos(window, xpos, ypos))
  }

  def glfwFreeCallbacks(window: Long): Unit = {
    runOnMainThread(Callbacks.glfwFreeCallbacks(window))
  }
}

class NullGlfw(config: WindowSystem.NullConfig) extends GlfwWrapper {
  private var errorCallback: GLFWErrorCallback = null.asInstanceOf[GLFWErrorCallback]

  def performCallsAsMainThread(): Unit = ()
  def glfwGetWindowPos(window: Long, xpos: Array[Int], ypos: Array[Int]): Unit = ()
  def glfwGetWindowSize(window: Long, xpos: Array[Int], ypos: Array[Int]): Unit = ()
  def glfwGetFramebufferSize(window: Long, xpos: Array[Int], ypos: Array[Int]): Unit = ()
  def glfwGetMonitorPos(monitor: Long, xpos: Array[Int], ypos: Array[Int]): Unit = ()
  def glfwGetCursorPos(window: Long, xpos: Array[Double], ypos: Array[Double]): Unit = ()
  def glfwGetMonitors(): PointerBuffer = PointerBuffer.allocateDirect(0)
  def glfwGetPrimaryMonitor(): Long = 123
  def glfwGetVideoMode(monitor: Long): VideoMode = VideoMode(2345, 1234, 10, 8, 6, 40)
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
  def glfwSetCursorPosCallback(window: Long, callback: GLFWCursorPosCallbackI): Unit = ()
  def glfwSetWindowSizeCallback(window: Long, callback: GLFWWindowSizeCallbackI): Unit = ()
  def glfwSetWindowFocusCallback(window: Long, callback: GLFWWindowFocusCallbackI): Unit = ()
  def glfwSetFramebufferSizeCallback(window: Long, callback: GLFWFramebufferSizeCallbackI): Unit = ()
  def glfwSetScrollCallback(window: Long, callback: GLFWScrollCallbackI): Unit = ()
  def glfwGetKey(window: Long, key: Int): Int = GLFW.GLFW_RELEASE
  def glfwSetInputMode(window: Long, mode: Int, value: Int): Unit = ()
  def glfwWindowShouldClose(window: Long): Boolean = config.shouldClose
  def glfwSwapBuffers(window: Long): Unit = ()
  def glfwSetWindowTitle(window: Long, title: String): Unit = ()
  def glfwPollEvents(): Unit = ()
  def glfwSwapInterval(interval: Int): Unit = ()
  def glfwDestroyWindow(window: Long): Unit = ()
  def glfwTerminate(): Unit = ()
  def glfwSetErrorCallback(callback: GLFWErrorCallbackI): GLFWErrorCallback = {
    val r = errorCallback
    errorCallback = if callback == null then null else GLFWErrorCallback.create(callback)
    r
  }
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

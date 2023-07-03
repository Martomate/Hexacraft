package com.martomate.hexacraft.infra

import com.martomate.hexacraft.util.PointerWrapper

import java.nio.ByteBuffer
import org.lwjgl.PointerBuffer
import org.lwjgl.glfw.*
import org.lwjgl.system.MemoryUtil
import scala.collection.mutable

object WindowSystem {
  def create(): WindowSystem = new WindowSystem(RealGlfw)
  def createNull(): WindowSystem = new WindowSystem(new NullGlfw)
}

class WindowSystem(glfw: GlfwWrapper) {
  def shutdown(): Unit =
    glfw.glfwTerminate()
    glfw.glfwSetErrorCallback(null).free()

  /** Initialize GLFW. Most GLFW functions will not work before doing this. */
  def initialize(): Unit =
    if !glfw.glfwInit()
    then throw new IllegalStateException("Unable to initialize GLFW")

  def setErrorCallback(callback: ErrorEvent => Unit): Unit =
    glfw.glfwSetErrorCallback((error, descriptionPointer) => {
      val description = MemoryUtil.memUTF8(descriptionPointer)
      val reason = WindowErrorReason.fromGlfw(error)
      callback(ErrorEvent(reason, description))
    })

  /** Poll for window events. The callbacks will (on most systems) only be invoked during this call. */
  def runEventCallbacks(): Unit = glfw.glfwPollEvents()

  /** Enables/disables vsync for the current context */
  def setVsync(enabled: Boolean): Unit = glfw.glfwSwapInterval(if enabled then 1 else 0)

  def primaryMonitor: Monitor = new Monitor(MonitorId(glfw.glfwGetPrimaryMonitor()), glfw)

  def monitors: Seq[Monitor] =
    val res = mutable.ArrayBuffer.empty[Monitor]

    val monitors = glfw.glfwGetMonitors()
    while monitors.hasRemaining
    do res += new Monitor(MonitorId(monitors.get), glfw)

    res.toSeq

  def createWindow(settings: WindowSettings): Option[Window] =
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
    if id != 0 then Some(new Window(WindowId(id), glfw)) else None
}

opaque type MonitorId <: AnyVal = Long
object MonitorId {
  def apply(id: Long): MonitorId = id
  extension (id: MonitorId) def toLong: Long = id
}

class Monitor(val id: MonitorId, glfw: GlfwWrapper) {
  private val pointerWrapper = new PointerWrapper()

  def position: (Int, Int) =
    pointerWrapper.ints((px, py) => glfw.glfwGetMonitorPos(id.toLong, px, py))

  def videoMode: VideoMode = glfw.glfwGetVideoMode(id.toLong)
}

opaque type WindowId <: AnyVal = Long
object WindowId {
  def apply(id: Long): WindowId = id
  extension (id: WindowId) def toLong: Long = id
}

class Window(val id: WindowId, glfw: GlfwWrapper) {
  private val pointerWrapper = new PointerWrapper()

  def position: (Int, Int) = pointerWrapper.ints((px, py) => glfw.glfwGetWindowPos(id.toLong, px, py))

  def size: (Int, Int) = pointerWrapper.ints((px, py) => glfw.glfwGetWindowSize(id.toLong, px, py))

  def framebufferSize: (Int, Int) = pointerWrapper.ints((px, py) => glfw.glfwGetFramebufferSize(id.toLong, px, py))

  def cursorPosition: (Double, Double) = pointerWrapper.doubles((px, py) => glfw.glfwGetCursorPos(id.toLong, px, py))

  def shouldClose: Boolean = glfw.glfwWindowShouldClose(id.toLong)

  def requestClose(): Unit = glfw.glfwSetWindowShouldClose(id.toLong, true)

  def close(): Unit =
    glfw.glfwFreeCallbacks(id.toLong)
    glfw.glfwDestroyWindow(id.toLong)

  def makeContextCurrent(): Unit = glfw.glfwMakeContextCurrent(id.toLong)

  def show(): Unit = glfw.glfwShowWindow(id.toLong)

  def moveTo(x: Int, y: Int): Unit = glfw.glfwSetWindowPos(id.toLong, x, y)

  def setTitle(title: String): Unit = glfw.glfwSetWindowTitle(id.toLong, title)

  def isKeyPressed(key: Int): Boolean = glfw.glfwGetKey(id.toLong, key) == GLFW.GLFW_PRESS

  def setCursorMode(mode: CursorMode): Unit =
    glfw.glfwSetInputMode(id.toLong, GLFW.GLFW_CURSOR, mode.toGlfw)

  def swapBuffers(): Unit = glfw.glfwSwapBuffers(id.toLong)

  def enterFullscreenMode(monitor: Monitor): Unit =
    val mode = monitor.videoMode
    glfw.glfwSetWindowMonitor(id.toLong, monitor.id.toLong, 0, 0, mode.width, mode.height, mode.refreshRate)

  def enterWindowedMode(x: Int, y: Int, width: Int, height: Int): Unit =
    glfw.glfwSetWindowMonitor(id.toLong, 0, x, y, width, height, GLFW.GLFW_DONT_CARE)

  def setKeyCallback(callback: CallbackEvent.KeyPressed => Unit): Unit =
    glfw.glfwSetKeyCallback(
      id.toLong,
      (_, key, scancode, action, mods) =>
        callback(
          CallbackEvent.KeyPressed(this, key, scancode, KeyAction.fromGlfw(action), KeyMods.fromGlfw(mods))
        )
    )

  def setCharCallback(callback: CallbackEvent.CharTyped => Unit): Unit =
    glfw.glfwSetCharCallback(
      id.toLong,
      (_, character) => callback(CallbackEvent.CharTyped(this, character))
    )

  def setMouseButtonCallback(callback: CallbackEvent.MouseClicked => Unit): Unit =
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

  def setWindowSizeCallback(callback: CallbackEvent.WindowResized => Unit): Unit =
    glfw.glfwSetWindowSizeCallback(
      id.toLong,
      (_, width, height) => callback(CallbackEvent.WindowResized(this, width, height))
    )

  def setFramebufferSizeCallback(callback: CallbackEvent.FramebufferResized => Unit): Unit =
    glfw.glfwSetFramebufferSizeCallback(
      id.toLong,
      (_, width, height) => callback(CallbackEvent.FramebufferResized(this, width, height))
    )

  def setScrollCallback(callback: CallbackEvent.MouseScrolled => Unit): Unit =
    glfw.glfwSetScrollCallback(
      id.toLong,
      (_, dx, dy) => callback(CallbackEvent.MouseScrolled(this, dx, dy))
    )
}

enum CursorMode:
  case Normal
  case Hidden
  case Disabled

object CursorMode {
  extension (mode: CursorMode)
    def toGlfw: Int = mode match
      case Normal   => GLFW.GLFW_CURSOR_NORMAL
      case Hidden   => GLFW.GLFW_CURSOR_HIDDEN
      case Disabled => GLFW.GLFW_CURSOR_DISABLED
}

case class WindowSettings(
    width: Int,
    height: Int,
    title: String,
    opengl: WindowSettings.Opengl,
    resizable: Boolean,
    samples: Int
)

object WindowSettings {
  case class Opengl(majorVersion: Int, minorVersion: Int, debugMode: Boolean)
}

enum KeyAction:
  case Press
  case Release
  case Repeat

object KeyAction:
  def fromGlfw(action: Int): KeyAction =
    action match
      case GLFW.GLFW_PRESS   => KeyAction.Press
      case GLFW.GLFW_RELEASE => KeyAction.Release
      case GLFW.GLFW_REPEAT  => KeyAction.Repeat

opaque type KeyMods = Int

object KeyMods:
  def fromGlfw(mods: Int): KeyMods = mods

extension (mods: KeyMods)
  def shiftDown: Boolean = (mods & GLFW.GLFW_MOD_SHIFT) != 0
  def ctrlDown: Boolean = (mods & GLFW.GLFW_MOD_CONTROL) != 0
  def altDown: Boolean = (mods & GLFW.GLFW_MOD_ALT) != 0

enum MouseButton:
  case Left
  case Right
  case Middle
  case Other(button: Int)

object MouseButton:
  def fromGlfw(button: Int): MouseButton =
    button match
      case GLFW.GLFW_MOUSE_BUTTON_LEFT   => MouseButton.Left
      case GLFW.GLFW_MOUSE_BUTTON_RIGHT  => MouseButton.Right
      case GLFW.GLFW_MOUSE_BUTTON_MIDDLE => MouseButton.Middle
      case _                             => MouseButton.Other(button)

enum MouseAction:
  case Press
  case Release

object MouseAction:
  def fromGlfw(action: Int): MouseAction =
    action match
      case GLFW.GLFW_PRESS   => MouseAction.Press
      case GLFW.GLFW_RELEASE => MouseAction.Release

enum CallbackEvent:
  case KeyPressed(window: Window, key: Int, scancode: Int, action: KeyAction, mods: KeyMods)
  case CharTyped(window: Window, character: Int)
  case MouseClicked(window: Window, button: MouseButton, action: MouseAction, mods: KeyMods)
  case MouseScrolled(window: Window, xOffset: Double, yOffset: Double)
  case WindowResized(window: Window, w: Int, h: Int)
  case FramebufferResized(window: Window, w: Int, h: Int)

enum WindowErrorReason:
  case NotInitialized
  case NoCurrentContext
  case InvalidEnum
  case InvalidValue
  case OutOfMemory
  case ApiUnavailable
  case VersionUnavailable
  case PlatformError
  case FormatUnavailable
  case NoWindowContext
  case CursorUnavailable
  case FeatureUnavailable
  case FeatureUnimplemented
  case PlatformUnavailable
  case Unknown(code: Int)

object WindowErrorReason {
  def fromGlfw(code: Int): WindowErrorReason = code match
    case GLFW.GLFW_NOT_INITIALIZED       => WindowErrorReason.NotInitialized
    case GLFW.GLFW_NO_CURRENT_CONTEXT    => WindowErrorReason.NoCurrentContext
    case GLFW.GLFW_INVALID_ENUM          => WindowErrorReason.InvalidEnum
    case GLFW.GLFW_INVALID_VALUE         => WindowErrorReason.InvalidValue
    case GLFW.GLFW_OUT_OF_MEMORY         => WindowErrorReason.OutOfMemory
    case GLFW.GLFW_API_UNAVAILABLE       => WindowErrorReason.ApiUnavailable
    case GLFW.GLFW_VERSION_UNAVAILABLE   => WindowErrorReason.VersionUnavailable
    case GLFW.GLFW_PLATFORM_ERROR        => WindowErrorReason.PlatformError
    case GLFW.GLFW_FORMAT_UNAVAILABLE    => WindowErrorReason.FormatUnavailable
    case GLFW.GLFW_NO_WINDOW_CONTEXT     => WindowErrorReason.NoWindowContext
    case GLFW.GLFW_CURSOR_UNAVAILABLE    => WindowErrorReason.CursorUnavailable
    case GLFW.GLFW_FEATURE_UNAVAILABLE   => WindowErrorReason.FeatureUnavailable
    case GLFW.GLFW_FEATURE_UNIMPLEMENTED => WindowErrorReason.FeatureUnimplemented
    case GLFW.GLFW_PLATFORM_UNAVAILABLE  => WindowErrorReason.PlatformUnavailable
    case _                               => WindowErrorReason.Unknown(code)
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
  def glfwGetWindowPos(window: Long, xpos: Array[Int], ypos: Array[Int]): Unit =
    GLFW.glfwGetWindowPos(window, xpos, ypos)

  def glfwGetWindowSize(window: Long, xpos: Array[Int], ypos: Array[Int]): Unit =
    GLFW.glfwGetWindowSize(window, xpos, ypos)

  def glfwGetFramebufferSize(window: Long, xpos: Array[Int], ypos: Array[Int]): Unit =
    GLFW.glfwGetFramebufferSize(window, xpos, ypos)

  def glfwGetMonitorPos(monitor: Long, xpos: Array[Int], ypos: Array[Int]): Unit =
    GLFW.glfwGetMonitorPos(monitor, xpos, ypos)

  def glfwGetCursorPos(window: Long, xpos: Array[Double], ypos: Array[Double]): Unit =
    GLFW.glfwGetCursorPos(window, xpos, ypos)

  def glfwGetMonitors(): PointerBuffer = GLFW.glfwGetMonitors()

  def glfwGetPrimaryMonitor(): Long = GLFW.glfwGetPrimaryMonitor()

  def glfwGetVideoMode(monitor: Long): VideoMode =
    val mode = GLFW.glfwGetVideoMode(monitor)
    VideoMode(mode.width, mode.height, mode.redBits, mode.greenBits, mode.blueBits, mode.refreshRate)

  def glfwSetWindowMonitor(
      window: Long,
      monitor: Long,
      xpos: Int,
      ypos: Int,
      width: Int,
      height: Int,
      refreshRate: Int
  ): Unit = GLFW.glfwSetWindowMonitor(window, monitor, xpos, ypos, width, height, refreshRate)

  def glfwSetKeyCallback(window: Long, callback: GLFWKeyCallbackI): Unit =
    GLFW.glfwSetKeyCallback(window, callback)

  def glfwSetCharCallback(window: Long, callback: GLFWCharCallbackI): Unit =
    GLFW.glfwSetCharCallback(window, callback)

  def glfwSetMouseButtonCallback(window: Long, callback: GLFWMouseButtonCallbackI): Unit =
    GLFW.glfwSetMouseButtonCallback(window, callback)

  def glfwSetWindowSizeCallback(window: Long, callback: GLFWWindowSizeCallbackI): Unit =
    GLFW.glfwSetWindowSizeCallback(window, callback)

  def glfwSetFramebufferSizeCallback(window: Long, callback: GLFWFramebufferSizeCallbackI): Unit =
    GLFW.glfwSetFramebufferSizeCallback(window, callback)

  def glfwSetScrollCallback(window: Long, callback: GLFWScrollCallbackI): Unit =
    GLFW.glfwSetScrollCallback(window, callback)

  def glfwGetKey(window: Long, key: Int): Int = GLFW.glfwGetKey(window, key)

  def glfwSetInputMode(window: Long, mode: Int, value: Int): Unit =
    GLFW.glfwSetInputMode(window, mode, value)

  def glfwWindowShouldClose(window: Long): Boolean = GLFW.glfwWindowShouldClose(window)

  def glfwSwapBuffers(window: Long): Unit = GLFW.glfwSwapBuffers(window)

  def glfwSetWindowTitle(window: Long, title: String): Unit =
    GLFW.glfwSetWindowTitle(window, title)

  def glfwPollEvents(): Unit = GLFW.glfwPollEvents()

  def glfwSwapInterval(interval: Int): Unit = GLFW.glfwSwapInterval(interval)

  def glfwDestroyWindow(window: Long): Unit = GLFW.glfwDestroyWindow(window)

  def glfwTerminate(): Unit = GLFW.glfwTerminate()

  def glfwSetErrorCallback(callback: GLFWErrorCallbackI): GLFWErrorCallback =
    GLFW.glfwSetErrorCallback(callback)

  def glfwInit(): Boolean = GLFW.glfwInit()

  def glfwMakeContextCurrent(window: Long): Unit = GLFW.glfwMakeContextCurrent(window)

  def glfwShowWindow(window: Long): Unit = GLFW.glfwShowWindow(window)

  def glfwCreateWindow(
      width: Int,
      height: Int,
      title: String,
      monitor: Long,
      share: Long
  ): Long = GLFW.glfwCreateWindow(width, height, title, monitor, share)

  def glfwDefaultWindowHints(): Unit =
    GLFW.glfwDefaultWindowHints()

  def glfwWindowHint(hint: Int, value: Int): Unit =
    GLFW.glfwWindowHint(hint, value)

  def glfwSetWindowShouldClose(window: Long, value: Boolean): Unit =
    GLFW.glfwSetWindowShouldClose(window, value)

  def glfwSetWindowPos(window: Long, xpos: Int, ypos: Int): Unit =
    GLFW.glfwSetWindowPos(window, xpos, ypos)

  def glfwFreeCallbacks(window: Long): Unit = Callbacks.glfwFreeCallbacks(window)
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

package com.martomate.hexacraft.infra

import com.martomate.hexacraft.util.PointerWrapper

import java.nio.ByteBuffer
import org.lwjgl.PointerBuffer
import org.lwjgl.glfw.{
  Callbacks,
  GLFW,
  GLFWCharCallbackI,
  GLFWErrorCallback,
  GLFWErrorCallbackI,
  GLFWFramebufferSizeCallbackI,
  GLFWKeyCallbackI,
  GLFWMouseButtonCallbackI,
  GLFWScrollCallbackI,
  GLFWVidMode,
  GLFWWindowSizeCallbackI
}
import org.lwjgl.system.MemoryUtil
import scala.collection.mutable

object WindowSystem {
  def create(): WindowSystem = new WindowSystem(RealGlfw)
  def createNull(): WindowSystem = new WindowSystem(new NullGlfw)
}

class WindowSystem(glfw: GlfwWrapper) {
  private val pointerWrapper = new PointerWrapper()

  def getWindowPos(window: Long): (Int, Int) =
    pointerWrapper.ints((px, py) => glfw.glfwGetWindowPos(window, px, py))

  def getWindowSize(window: Long): (Int, Int) =
    pointerWrapper.ints((px, py) => glfw.glfwGetWindowSize(window, px, py))

  def getFramebufferSize(window: Long): (Int, Int) =
    pointerWrapper.ints((px, py) => glfw.glfwGetFramebufferSize(window, px, py))

  def getMonitorPos(window: Long): (Int, Int) =
    pointerWrapper.ints((px, py) => glfw.glfwGetMonitorPos(window, px, py))

  def getCursorPos(window: Long): (Double, Double) =
    pointerWrapper.doubles((px, py) => glfw.glfwGetCursorPos(window, px, py))

  def monitors: Seq[Long] =
    val res = mutable.ArrayBuffer.empty[Long]

    val monitors = glfw.glfwGetMonitors()
    while monitors.hasRemaining
    do res += monitors.get

    res.toSeq

  def primaryMonitor: Long = glfw.glfwGetPrimaryMonitor()

  def getVideoMode(monitor: Long): VideoMode = glfw.glfwGetVideoMode(monitor)

  def glfwSetWindowMonitor(
      window: Long,
      monitor: Long,
      xpos: Int,
      ypos: Int,
      width: Int,
      height: Int,
      refreshRate: Int
  ): Unit = glfw.glfwSetWindowMonitor(window, monitor, xpos, ypos, width, height, refreshRate)

  def setKeyCallback(window: Long, callback: CallbackEvent.KeyPressed => Unit): Unit =
    glfw.glfwSetKeyCallback(
      window,
      (window, key, scancode, action, mods) =>
        callback(CallbackEvent.KeyPressed(window, key, scancode, KeyAction.fromGlfw(action), KeyMods.fromGlfw(mods)))
    )

  def setCharCallback(window: Long, callback: CallbackEvent.CharTyped => Unit): Unit =
    glfw.glfwSetCharCallback(window, (window, character) => callback(CallbackEvent.CharTyped(window, character)))

  def setMouseButtonCallback(window: Long, callback: CallbackEvent.MouseClicked => Unit): Unit =
    glfw.glfwSetMouseButtonCallback(
      window,
      (window, button, action, mods) =>
        callback(
          CallbackEvent.MouseClicked(
            window,
            MouseButton.fromGlfw(button),
            MouseAction.fromGlfw(action),
            KeyMods.fromGlfw(mods)
          )
        )
    )

  def setWindowSizeCallback(window: Long, callback: CallbackEvent.WindowResized => Unit): Unit =
    glfw.glfwSetWindowSizeCallback(
      window,
      (window, width, height) => callback(CallbackEvent.WindowResized(window, width, height))
    )

  def setFramebufferSizeCallback(window: Long, callback: CallbackEvent.FramebufferResized => Unit): Unit =
    glfw.glfwSetFramebufferSizeCallback(
      window,
      (window, width, height) => callback(CallbackEvent.FramebufferResized(window, width, height))
    )

  def setScrollCallback(window: Long, callback: CallbackEvent.MouseScrolled => Unit): Unit =
    glfw.glfwSetScrollCallback(window, (window, dx, dy) => callback(CallbackEvent.MouseScrolled(window, dx, dy)))

  def setErrorCallback(callback: ErrorEvent => Unit): GLFWErrorCallback =
    glfw.glfwSetErrorCallback((error, descriptionPointer) => {
      val description = MemoryUtil.memUTF8(descriptionPointer)
      val reason = WindowErrorReason.fromGlfw(error)
      callback(ErrorEvent(reason, description))
    })

  def isKeyPressed(window: Long, key: Int): Boolean = glfw.glfwGetKey(window, key) == GLFW.GLFW_PRESS

  def glfwSetInputMode(window: Long, mode: Int, value: Int): Unit =
    glfw.glfwSetInputMode(window, mode, value)

  def glfwWindowShouldClose(window: Long): Boolean = glfw.glfwWindowShouldClose(window)

  def swapBuffers(window: Long): Unit = glfw.glfwSwapBuffers(window)

  def setWindowTitle(window: Long, title: String): Unit =
    glfw.glfwSetWindowTitle(window, title)

  def glfwPollEvents(): Unit = glfw.glfwPollEvents()

  def glfwSwapInterval(interval: Int): Unit = glfw.glfwSwapInterval(interval)

  def glfwDestroyWindow(window: Long): Unit = glfw.glfwDestroyWindow(window)

  def glfwTerminate(): Unit = glfw.glfwTerminate()

  def glfwInit(): Boolean = glfw.glfwInit()

  def glfwMakeContextCurrent(window: Long): Unit = glfw.glfwMakeContextCurrent(window)

  def glfwShowWindow(window: Long): Unit = glfw.glfwShowWindow(window)

  def glfwCreateWindow(
      width: Int,
      height: Int,
      title: String,
      monitor: Long,
      share: Long
  ): Long = glfw.glfwCreateWindow(width, height, title, monitor, share)

  def glfwDefaultWindowHints(): Unit =
    glfw.glfwDefaultWindowHints()

  def glfwWindowHint(hint: Int, value: Int): Unit =
    glfw.glfwWindowHint(hint, value)

  def glfwSetWindowShouldClose(window: Long, value: Boolean): Unit =
    glfw.glfwSetWindowShouldClose(window, value)

  def setWindowPos(window: Long, xpos: Int, ypos: Int): Unit =
    glfw.glfwSetWindowPos(window, xpos, ypos)

  def glfwFreeCallbacks(window: Long): Unit = glfw.glfwFreeCallbacks(window)
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
  case KeyPressed(window: Long, key: Int, scancode: Int, action: KeyAction, mods: KeyMods)
  case CharTyped(window: Long, character: Int)
  case MouseClicked(window: Long, button: MouseButton, action: MouseAction, mods: KeyMods)
  case MouseScrolled(window: Long, xOffset: Double, yOffset: Double)
  case WindowResized(window: Long, w: Int, h: Int)
  case FramebufferResized(window: Long, w: Int, h: Int)

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
  def glfwGetMonitorPos(window: Long, xpos: Array[Int], ypos: Array[Int]): Unit
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

  def glfwGetMonitorPos(window: Long, xpos: Array[Int], ypos: Array[Int]): Unit =
    GLFW.glfwGetMonitorPos(window, xpos, ypos)

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
  def glfwGetMonitorPos(window: Long, xpos: Array[Int], ypos: Array[Int]): Unit = ()
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

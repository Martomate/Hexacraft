package com.martomate.hexacraft.main

import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL43
import org.lwjgl.system.MemoryUtil
import scala.collection.mutable

enum CallbackEvent:
  /** @param action 0 = release, 1 = press, 2 = repeat */
  case KeyPressed(window: Long, key: Int, scancode: Int, action: Int, mods: Int)
  case CharTyped(window: Long, character: Int)

  /** @param mods 1 = Shift, 2 = Ctrl, 4 = Alt. These are combined with | */
  case MouseClicked(window: Long, button: Int, action: Int, mods: Int)
  case MouseScrolled(window: Long, xoffset: Double, yoffset: Double)
  case WindowResized(window: Long, w: Int, h: Int)
  case FramebufferResized(window: Long, w: Int, h: Int)
  case DebugMessage(
      source: Int,
      debugType: Int,
      id: Int,
      severity: Int,
      message: String,
      userParam: Long
  )

class CallbackHandler:
  private val callbackQueue = mutable.Queue.empty[CallbackEvent]

  def handle(handler: CallbackEvent => Unit): Unit =
    if callbackQueue.nonEmpty
    then
      while callbackQueue.nonEmpty
      do handler(callbackQueue.dequeue())

  private def onKeyCallback(window: Long, key: Int, scancode: Int, action: Int, mods: Int): Unit =
    callbackQueue.enqueue(CallbackEvent.KeyPressed(window, key, scancode, action, mods))

  private def onCharCallback(window: Long, character: Int): Unit =
    callbackQueue.enqueue(CallbackEvent.CharTyped(window, character))

  private def onMouseButtonCallback(window: Long, button: Int, action: Int, mods: Int): Unit =
    callbackQueue.enqueue(CallbackEvent.MouseClicked(window, button, action, mods))

  private def onWindowSizeCallback(window: Long, width: Int, height: Int): Unit =
    callbackQueue.enqueue(CallbackEvent.WindowResized(window, width, height))

  private def onFramebufferSizeCallback(window: Long, width: Int, height: Int): Unit =
    callbackQueue.enqueue(CallbackEvent.FramebufferResized(window, width, height))

  private def onScrollCallback(window: Long, dx: Double, dy: Double): Unit =
    callbackQueue.enqueue(CallbackEvent.MouseScrolled(window, dx, dy))

  private def onDebugMessageCallback(
      source: Int,
      debugType: Int,
      id: Int,
      severity: Int,
      length: Int,
      messageAddress: Long,
      userParam: Long
  ): Unit =
    val message =
      if length < 0
      then MemoryUtil.memASCII(messageAddress)
      else MemoryUtil.memASCII(messageAddress, length)
    callbackQueue.enqueue(CallbackEvent.DebugMessage(source, debugType, id, severity, message, userParam))

  def addKeyCallback(window: Long): Unit = GLFW.glfwSetKeyCallback(window, onKeyCallback)

  def addCharCallback(window: Long): Unit = GLFW.glfwSetCharCallback(window, onCharCallback)

  def addMouseButtonCallback(window: Long): Unit =
    GLFW.glfwSetMouseButtonCallback(window, onMouseButtonCallback)

  def addWindowSizeCallback(window: Long): Unit =
    GLFW.glfwSetWindowSizeCallback(window, onWindowSizeCallback)

  def addFramebufferSizeCallback(window: Long): Unit =
    GLFW.glfwSetFramebufferSizeCallback(window, onFramebufferSizeCallback)

  def addScrollCallback(window: Long): Unit = GLFW.glfwSetScrollCallback(window, onScrollCallback)

  def addDebugMessageCallback(): Unit = GL43.glDebugMessageCallback(onDebugMessageCallback, 0L)

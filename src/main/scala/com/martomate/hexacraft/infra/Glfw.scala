package com.martomate.hexacraft.infra

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

object Glfw {
  def create(): Glfw = new Glfw
}

class Glfw {
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

  def glfwGetVideoMode(monitor: Long): GLFWVidMode = GLFW.glfwGetVideoMode(monitor)

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
    GLFW.glfwSetErrorCallback(null)

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

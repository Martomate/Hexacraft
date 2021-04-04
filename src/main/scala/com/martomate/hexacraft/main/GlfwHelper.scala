package com.martomate.hexacraft.main

import com.martomate.hexacraft.util.PointerWrapper
import org.lwjgl.glfw.GLFW

class GlfwHelper {
  private val pointerWrapper = new PointerWrapper()

  def getWindowPos(window: Long): (Int, Int) = {
    pointerWrapper.ints((px, py) => GLFW.glfwGetWindowPos(window, px, py))
  }

  def getWindowSize(window: Long): (Int, Int) = {
    pointerWrapper.ints((px, py) => GLFW.glfwGetWindowSize(window, px, py))
  }

  def getMonitorPos(window: Long): (Int, Int) = {
    pointerWrapper.ints((px, py) => GLFW.glfwGetMonitorPos(window, px, py))
  }

  def getCursorPos(window: Long): (Double, Double) = {
    pointerWrapper.doubles((px, py) => GLFW.glfwGetCursorPos(window, px, py))
  }

  /** Determines the current monitor that the specified window is being displayed on.
    * If the monitor could not be determined, the primary monitor will be returned.
    */
  def getCurrentMonitor(window: Long): Long = {
    val (wx, wy) = getWindowPos(window)
    val (ww, wh) = getWindowSize(window)

    getCurrentMonitor(wx, wy, ww, wh)
  }

  def getCurrentMonitor(windowPosX: Int, windowPosY: Int, windowWidth: Int, windowHeight: Int): Long = {
    var bestOverlap = 0
    var bestMonitor = 0L

    val monitors = GLFW.glfwGetMonitors()
    while (monitors.hasRemaining) {
      val monitor = monitors.get

      val (monitorPosX, monitorPosY) = getMonitorPos(monitor)

      val mode = GLFW.glfwGetVideoMode(monitor)
      val monitorWidth = mode.width
      val monitorHeight = mode.height

      val overlapRight = Math.min(windowPosX + windowWidth, monitorPosX + monitorWidth)
      val overlapLeft = Math.max(windowPosX, monitorPosX)
      val overlapBottom = Math.min(windowPosY + windowHeight, monitorPosY + monitorHeight)
      val overlapTop = Math.max(windowPosY, monitorPosY)

      val overlapWidth = Math.max(0, overlapRight - overlapLeft)
      val overlapHeight = Math.max(0, overlapBottom - overlapTop)
      val overlap = overlapWidth * overlapHeight

      if (bestOverlap < overlap) {
        bestOverlap = overlap
        bestMonitor = monitor
      }
    }
    if (bestMonitor != 0L) bestMonitor else GLFW.glfwGetPrimaryMonitor()
  }

}

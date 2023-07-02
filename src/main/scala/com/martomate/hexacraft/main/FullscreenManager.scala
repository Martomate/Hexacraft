package com.martomate.hexacraft.main

import com.martomate.hexacraft.infra.Glfw

import org.joml.Vector2i
import org.lwjgl.glfw.GLFW

class FullscreenManager(window: Long, glfw: Glfw):
  private var fullscreen = false
  private val prevWindowPos = new Vector2i()
  private val prevWindowSize = new Vector2i()

  def isFullscreen: Boolean = fullscreen

  def toggleFullscreen(): Unit =
    if fullscreen
    then setWindowed()
    else setFullscreen()

    fullscreen = !fullscreen

  private def setWindowed(): Unit =
    val (wx, wy) = (prevWindowPos.x, prevWindowPos.y)
    val (ww, wh) = (prevWindowSize.x, prevWindowSize.y)

    glfw.glfwSetWindowMonitor(window, 0, wx, wy, ww, wh, GLFW.GLFW_DONT_CARE)

  private def setFullscreen(): Unit =
    val (wx, wy) = glfw.getWindowPos(window)
    val (ww, wh) = glfw.getWindowSize(window)

    prevWindowPos.set(wx, wy)
    prevWindowSize.set(ww, wh)

    val monitor = getCurrentMonitor(wx, wy, ww, wh)
    val mode = glfw.getVideoMode(monitor)

    glfw.glfwSetWindowMonitor(window, monitor, 0, 0, mode.width, mode.height, mode.refreshRate)

  private def getCurrentMonitor(
      windowPosX: Int,
      windowPosY: Int,
      windowWidth: Int,
      windowHeight: Int
  ): Long =
    var bestOverlap = 0
    var bestMonitor = 0L

    for monitor <- glfw.monitors do
      val (monitorPosX, monitorPosY) = glfw.getMonitorPos(monitor)

      val mode = glfw.getVideoMode(monitor)
      val monitorWidth = mode.width
      val monitorHeight = mode.height

      val overlapRight = Math.min(windowPosX + windowWidth, monitorPosX + monitorWidth)
      val overlapLeft = Math.max(windowPosX, monitorPosX)
      val overlapBottom = Math.min(windowPosY + windowHeight, monitorPosY + monitorHeight)
      val overlapTop = Math.max(windowPosY, monitorPosY)

      val overlapWidth = Math.max(0, overlapRight - overlapLeft)
      val overlapHeight = Math.max(0, overlapBottom - overlapTop)
      val overlap = overlapWidth * overlapHeight

      if bestOverlap < overlap
      then
        bestOverlap = overlap
        bestMonitor = monitor

    if bestMonitor != 0L
    then bestMonitor
    else glfw.primaryMonitor

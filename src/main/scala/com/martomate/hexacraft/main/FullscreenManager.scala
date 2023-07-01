package com.martomate.hexacraft.main

import com.martomate.hexacraft.infra.Glfw

import org.joml.Vector2i
import org.lwjgl.glfw.GLFW

class FullscreenManager(window: Long, glfw: Glfw):
  private var fullscreen = false
  private val prevWindowPos = new Vector2i()
  private val prevWindowSize = new Vector2i()

  private val glfwHelper = new GlfwHelper(glfw)

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
    val (wx, wy) = glfwHelper.getWindowPos(window)
    val (ww, wh) = glfwHelper.getWindowSize(window)

    prevWindowPos.set(wx, wy)
    prevWindowSize.set(ww, wh)

    val monitor = glfwHelper.getCurrentMonitor(wx, wy, ww, wh)
    val mode = glfw.glfwGetVideoMode(monitor)

    glfw.glfwSetWindowMonitor(
      window,
      monitor,
      0,
      0,
      mode.width(),
      mode.height(),
      mode.refreshRate()
    )

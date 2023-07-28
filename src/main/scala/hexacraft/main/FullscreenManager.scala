package hexacraft.main

import hexacraft.infra.window.{Monitor, Window, WindowSystem}
import org.joml.Vector2i

class FullscreenManager(window: Window, windowSystem: WindowSystem):
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

    window.enterWindowedMode(wx, wy, ww, wh)

  private def setFullscreen(): Unit =
    val (wx, wy) = window.position
    val (ww, wh) = window.size

    prevWindowPos.set(wx, wy)
    prevWindowSize.set(ww, wh)

    val monitor = getCurrentMonitor(wx, wy, ww, wh)
    window.enterFullscreenMode(monitor)

  private def getCurrentMonitor(
      windowPosX: Int,
      windowPosY: Int,
      windowWidth: Int,
      windowHeight: Int
  ): Monitor =
    var bestOverlap = 0
    var bestMonitor: Option[Monitor] = None

    for monitor <- windowSystem.monitors do
      val (monitorPosX, monitorPosY) = monitor.position

      val mode = monitor.videoMode
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
        bestMonitor = Some(monitor)

    bestMonitor.getOrElse(windowSystem.primaryMonitor)

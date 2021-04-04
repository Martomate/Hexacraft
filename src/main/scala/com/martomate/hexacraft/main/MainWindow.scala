package com.martomate.hexacraft.main

import com.martomate.hexacraft._
import com.martomate.hexacraft.event.{CharEvent, KeyEvent, MouseClickEvent, ScrollEvent}
import com.martomate.hexacraft.gui.comp.GUITransformation
import com.martomate.hexacraft.menu.main.MainMenu
import com.martomate.hexacraft.renderer.VAO
import com.martomate.hexacraft.resource.{Resource, Shader}
import com.martomate.hexacraft.scene.{GameWindowExtended, SceneStack}
import com.martomate.hexacraft.util.PointerWrapper
import com.martomate.hexacraft.util.os.OSUtils
import com.martomate.hexacraft.world.block.{BlockLoader, Blocks}
import org.joml.{Vector2d, Vector2dc, Vector2i, Vector2ic}
import org.lwjgl.glfw.GLFW._
import org.lwjgl.glfw.{Callbacks, GLFW, GLFWErrorCallback}
import org.lwjgl.opengl.{GL, GL11}

import java.io.File

class MainWindow extends GameWindowExtended {
  val saveFolder: File = new File(OSUtils.appdataPath, ".hexacraft")

  private val _windowSize = new Vector2i(960, 540)
  private val prevWindowSize = new Vector2i()
  private val prevWindowPos = new Vector2i()

  def windowSize: Vector2ic = _windowSize

  private val pointerWrapper = new PointerWrapper()

  private val window: Long = initWindow()
  private var fullscreen = false
  private var vsync = false
  private var skipMouseMovedUpdate = false

  private val _mousePos = new Vector2d()
  private val _mouseMoved = new Vector2d()

  override val mouse: GameMouse = new GameMouse {
    override def pos: Vector2dc = _mousePos

    override def moved: Vector2dc = _mouseMoved
  }

  override val keyboard: GameKeyboard = key => glfwGetKey(window, key)

  override def setCursorLayout(cursorLayout: Int): Unit = glfwSetInputMode(window, GLFW_CURSOR, cursorLayout)

  override val scenes: SceneStack = new SceneStackImpl

  def resetMousePos(): Unit = {
    val (cx, cy) = pointerWrapper.doubles((px, py) => glfwGetCursorPos(window, px, py))
    _mousePos.set(cx, _windowSize.y - cy)
    skipMouseMovedUpdate = true
  }

  private def loop(): Unit = {
    var prevTime = System.nanoTime
    var ticks, frames, fps, titleTicker = 0

    while (!glfwWindowShouldClose(window)) {
      val currentTime = System.nanoTime
      val delta = ((currentTime - prevTime) * 1e-9 * 60).toInt
      val realPrevTime = currentTime
      for (_ <- 0 until delta) {
        tick()
        ticks += 1
        titleTicker += 1
        if (ticks % 60 == 0) {
          fps = frames

          handleVsync(fps)

          frames = 0
        }
        prevTime += 1e9.toLong / 60
      }
      GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT)
      render()
      val error = GL11.glGetError
      if (error != GL11.GL_NO_ERROR) println("OpenGL error: " + error)

      frames += 1
      val realDeltaTime = System.nanoTime - realPrevTime
      val msTime = (realDeltaTime * 1e-6).toInt

      glfwSwapBuffers(window)

      if (titleTicker > 10) {
        titleTicker = 0
        val msString = (if (msTime < 10) "0" else "") + msTime
        val vsyncStr = if (vsync) "vsync" else ""
        val debugInfo = (
          scenes.map(_.windowTitle) :+
            s"$fps fps   ms: $msString" :+
            vsyncStr
        ).filter(_.nonEmpty).mkString("   |   ")

        glfwSetWindowTitle(window, "Hexacraft   |   " + debugInfo)
      }
      // Poll for window events. The key callback will only be
      // invoked during this call.
      glfwPollEvents()
    }
  }

  private def handleVsync(fps: Int): Unit = {
    val newVsync = shouldUseVsync(fps)

    if (newVsync != vsync) {
      vsync = newVsync
      glfwSwapInterval(if (vsync) 1 else 0)
    }
  }

  private def shouldUseVsync(fps: Int) = {
    if (fps > 80) true
    else if (fps < 50) false
    else vsync
  }

  private def render(): Unit = {
    def render(idx: Int): Unit = {
      if (idx > 0 && !scenes(idx).isOpaque) render(idx-1)

      scenes(idx).render(GUITransformation(0, 0))
    }
    render(scenes.size - 1)

    VAO.unbindVAO()
  }

  private def tick(): Unit = {
    val (cx, cy) = pointerWrapper.doubles((px, py) => glfwGetCursorPos(window, px, py))
    val oldMouseX = _mousePos.x
    val oldMouseY = _mousePos.y
    _mousePos.set(cx, _windowSize.y - cy)

    if (skipMouseMovedUpdate) {
      _mouseMoved.set(0, 0)
      skipMouseMovedUpdate = false
    } else {
      _mouseMoved.set(_mousePos.x - oldMouseX, _mousePos.y - oldMouseY)
    }

    scenes.foreach(_.tick()) // TODO: should maybe be reversed
  }

  def run(): Unit = {
    initGL()

    try {
      implicit val windowImplicit: GameWindowExtended = this
      Shader.init()
      BlockLoader.init()// this loads it to memory
      Blocks.init()
      scenes.pushScene(new MainMenu)
      resetMousePos()
      Shader.foreach(_.setUniform2f("windowSize", _windowSize.x.toFloat, _windowSize.y.toFloat))
      loop()
    } catch {
      case e: Exception =>
        e.printStackTrace()
    } finally {
      destroy()

      // Free the window callbacks and destroy the window
      Callbacks.glfwFreeCallbacks(window)
      glfwDestroyWindow(window)

      // Terminate GLFW and free the error callback
      glfwTerminate()
      glfwSetErrorCallback(null).free()
    }
  }

  private def processKeys(window: Long, key: Int, scancode: Int, action: Int, mods: Int): Unit = { // action: 0 = release, 1 = press, 2 = repeat
    if (key == GLFW_KEY_R && action == GLFW_PRESS && GLFW.glfwGetKey(window, GLFW_KEY_F3) == GLFW_PRESS) {
      Resource.reloadAllResources()
      scenes.foreach(_.onReloadedResources())
      println("Reloaded resources")
    }
    if (key == GLFW_KEY_F11 && action == GLFW_PRESS) {
      setFullscreen()
    }
    scenes.reverseIterator.exists(_.onKeyEvent(KeyEvent(key, scancode, action, mods)))
  }

  private def setFullscreen(): Unit = {
    val monitor = glfwGetCurrentMonitor(window)
    val mode = glfwGetVideoMode(monitor)

    if (!fullscreen) {
      prevWindowSize.set(_windowSize)
      val (wx, wy) = pointerWrapper.ints((px, py) => glfwGetWindowPos(window, px, py))
      prevWindowPos.set(wx, wy)

      glfwSetWindowMonitor(window, monitor, 0, 0, mode.width(), mode.height(), mode.refreshRate())
    } else {
      glfwSetWindowMonitor(window, 0, prevWindowPos.x, prevWindowPos.y, prevWindowSize.x, prevWindowSize.y, GLFW_DONT_CARE)
    }

    fullscreen = !fullscreen
    skipMouseMovedUpdate = true
  }

  /** Determines the current monitor that the specified window is being displayed on.
    * If the monitor could not be determined, the primary monitor will be returned.
    */
  def glfwGetCurrentMonitor(window: Long): Long = {
    val (wx, wy) = pointerWrapper.ints((px, py) => glfwGetWindowPos(window, px, py))
    val (ww, wh) = pointerWrapper.ints((px, py) => glfwGetWindowSize(window, px, py))

    glfwGetCurrentMonitor(wx, wy, ww, wh)
  }

  def glfwGetCurrentMonitor(windowPosX: Int, windowPosY: Int, windowWidth: Int, windowHeight: Int): Long = {
    var bestOverlap = 0
    var bestMonitor = 0L

    val monitors = glfwGetMonitors()
    while (monitors.hasRemaining) {
      val monitor = monitors.get

      val (monitorPosX, monitorPosY) = pointerWrapper.ints((px, py) => glfwGetMonitorPos(monitor, px, py))

      val mode = glfwGetVideoMode(monitor)
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
    if (bestMonitor != 0L) bestMonitor else glfwGetPrimaryMonitor()
  }

  private def processChar(window: Long, character: Int): Unit = {
    scenes.reverseIterator.exists(_.onCharEvent(CharEvent(character)))
  }

  /** @param mods 1 = Shift, 2 = Ctrl, 4 = Alt. These are combined with | */
  private def processMouseButtons(window: Long, button: Int, action: Int, mods: Int): Unit = {
    val mousePos = (normalizedMousePos.x * aspectRatio, normalizedMousePos.y)
    scenes.reverseIterator.exists(_.onMouseClickEvent(MouseClickEvent(button, action, mods, mousePos)))
  }

  private def processScroll(window: Long, xOffset: Double, yOffset: Double): Unit = {
    scenes.reverseIterator.exists(_.onScrollEvent(ScrollEvent(xOffset.toFloat, yOffset.toFloat)))
  }

  private def resizeWindow(width: Int, height: Int): Unit = {
    if (width > 0 && height > 0) {
      if (width != _windowSize.x || height != _windowSize.y) {
        GL11.glViewport(0, 0, width, height)

        scenes.foreach(_.windowResized(width, height))
      }
      _windowSize.set(width, height)
      skipMouseMovedUpdate = true
    }
    Shader.foreach(_.setUniform2f("windowSize", _windowSize.x.toFloat, _windowSize.y.toFloat))
  }

  private def initWindow(): Long = {
    // Setup an error callback. The default implementation
    // will print the error message in System.err.
    GLFWErrorCallback.createPrint(System.err).set

    // Initialize GLFW. Most GLFW functions will not work before doing this.
    if (!glfwInit) throw new IllegalStateException("Unable to initialize GLFW")

    configureGlfw()

    val window = glfwCreateWindow(_windowSize.x, _windowSize.y, "Hexacraft", 0, 0)
    if (window == 0) throw new RuntimeException("Failed to create the GLFW window")

    setupCallbacks(window)

    centerWindow(window)

    glfwMakeContextCurrent(window)
    // Enable v-sync
    glfwSwapInterval(if (vsync) 1 else 0)

    glfwShowWindow(window)
    window
  }

  private def configureGlfw(): Unit = {
    glfwDefaultWindowHints() // optional, the current window hints are already the default
    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE) // the window will stay hidden after creation
    glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE) // the window will be resizable
    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
    glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL11.GL_TRUE)
    glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
    glfwWindowHint(GLFW_SAMPLES, 1)
  }

  private def setupCallbacks(window: Long): Unit = {
    // Setup a key callback. It will be called every time a key is pressed, repeated or released.
    glfwSetKeyCallback(window, processKeys)
    glfwSetCharCallback(window, processChar)
    glfwSetMouseButtonCallback(window, processMouseButtons)
    glfwSetWindowSizeCallback(window, (_, width, height) => resizeWindow(width, height))
    glfwSetScrollCallback(window, processScroll)
  }

  private def centerWindow(window: Long): Unit = {
    val (windowWidth, windowHeight) = pointerWrapper.ints((px, py) => glfwGetWindowSize(window, px, py))

    val mode = glfwGetVideoMode(glfwGetPrimaryMonitor)

    glfwSetWindowPos(window, (mode.width - windowWidth) / 2, (mode.height - windowHeight) / 2)
  }

  private def initGL(): Unit = {
    GL.createCapabilities()

//  GL11.glEnable(GL13.GL_MULTISAMPLE)
    GL11.glEnable(GL11.GL_DEPTH_TEST)
    GL11.glDepthFunc(GL11.GL_LEQUAL)
    GL11.glEnable(GL11.GL_CULL_FACE)
  }

  def tryQuit(): Unit = {
    glfwSetWindowShouldClose(window, true)
  }

  def destroy(): Unit = {
    while (scenes.nonEmpty) scenes.popScene()

    Resource.freeAllResources()
  }
}


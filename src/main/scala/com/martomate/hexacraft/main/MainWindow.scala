package com.martomate.hexacraft.main

import java.io.File

import com.martomate.hexacraft._
import com.martomate.hexacraft.event.{CharEvent, KeyEvent, MouseClickEvent, ScrollEvent}
import com.martomate.hexacraft.gui.comp.GUITransformation
import com.martomate.hexacraft.menu.main.MainMenu
import com.martomate.hexacraft.renderer.VAO
import com.martomate.hexacraft.resource.{Resource, Shader}
import com.martomate.hexacraft.scene.{GameWindowExtended, SceneStack}
import com.martomate.hexacraft.util.os.OSUtils
import com.martomate.hexacraft.world.block.{BlockLoader, Blocks}
import org.joml.{Vector2d, Vector2dc, Vector2i, Vector2ic}
import org.lwjgl.glfw.GLFW._
import org.lwjgl.glfw.{Callbacks, GLFW, GLFWErrorCallback}
import org.lwjgl.opengl.{GL, GL11}
import org.lwjgl.system.{MemoryStack, MemoryUtil}

class MainWindow extends GameWindowExtended {
  val saveFolder: File = new File(OSUtils.appdataPath, ".hexacraft")

  private val _windowSize = new Vector2i(960, 540)
  private val prevWindowSize = new Vector2i()
  private val prevWindowPos = new Vector2i()

  def windowSize: Vector2ic = _windowSize

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

  private val doublePtrX = MemoryUtil.memAllocDouble(1)
  private val doublePtrY = MemoryUtil.memAllocDouble(1)
  private val intPtrX = MemoryUtil.memAllocInt(1)
  private val intPtrY = MemoryUtil.memAllocInt(1)
  def mousePos: Vector2dc = new Vector2d(_mousePos)
  def mouseMoved: Vector2dc = new Vector2d(_mouseMoved)

  override val scenes: SceneStack = new SceneStackImpl

  def resetMousePos(): Unit = {
    glfwGetCursorPos(window, doublePtrX, doublePtrY)
    _mousePos.set(doublePtrX.get(0), _windowSize.y - doublePtrY.get(0))
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
        ).filter(!_.isEmpty).mkString("   |   ")

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
      if (idx >= 0 && !scenes(idx).isOpaque) render(idx-1)

      scenes(idx).render(GUITransformation(0, 0))
    }
    render(scenes.size - 1)

    VAO.unbindVAO()
  }

  private def tick(): Unit = {
    glfwGetCursorPos(window, doublePtrX, doublePtrY)
    val oldMouseX = _mousePos.x
    val oldMouseY = _mousePos.y
    _mousePos.set(doublePtrX.get(0), _windowSize.y - doublePtrY.get(0))

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
    val monitor = glfwGetPrimaryMonitor()
    val mode = glfwGetVideoMode(monitor)

    if (!fullscreen) {
      prevWindowSize.set(_windowSize)
      glfwGetWindowPos(window, intPtrX, intPtrY)
      prevWindowPos.set(intPtrX.get(0), intPtrY.get(0))

      glfwSetWindowMonitor(window, monitor, 0, 0, mode.width(), mode.height(), mode.refreshRate())
    } else {
      glfwSetWindowMonitor(window, 0, prevWindowPos.x, prevWindowPos.y, prevWindowSize.x, prevWindowSize.y, GLFW_DONT_CARE)
    }

    fullscreen = !fullscreen
    skipMouseMovedUpdate = true
  }

  private def processChar(window: Long, character: Int): Unit = {
    scenes.reverseIterator.exists(_.onCharEvent(CharEvent(character)))
  }

  private def processMouseButtons(window: Long, button: Int, action: Int, mods: Int): Unit = { // mods: 1 = Shift, 2 = Ctrl, 4 = Alt. These are combined with |
    scenes.reverseIterator.exists(_.onMouseClickEvent(MouseClickEvent(button, action, mods, (normalizedMousePos.x * aspectRatio, normalizedMousePos.y))))
  }

  private def processScroll(window: Long, xoffset: Double, yoffset: Double): Unit = {
    scenes.reverseIterator.exists(_.onScrollEvent(ScrollEvent(xoffset.toFloat, yoffset.toFloat)))
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

    // Configure GLFW
    glfwDefaultWindowHints() // optional, the current window hints are already the default
    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE) // the window will stay hidden after creation
    glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE) // the window will be resizable
    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
    glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL11.GL_TRUE)
    glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
    glfwWindowHint(GLFW_SAMPLES, 1)

    val window = glfwCreateWindow(_windowSize.x, _windowSize.y, "Hexacraft", 0, 0)
    if (window == 0) throw new RuntimeException("Failed to create the GLFW window")

    // Setup a key callback. It will be called every time a key is pressed, repeated or released.
    glfwSetKeyCallback(window, processKeys)
    glfwSetCharCallback(window, processChar)
    glfwSetMouseButtonCallback(window, processMouseButtons)
    glfwSetWindowSizeCallback(window, (_, width, height) => resizeWindow(width, height))
    glfwSetScrollCallback(window, processScroll)

    // Get the thread stack and push a new frame
    {
      val stack = MemoryStack.stackPush()
      val pWidth = stack.mallocInt(1) // int*
      val pHeight = stack.mallocInt(1) // int*

      // Get the window size passed to glfwCreateWindow
      glfwGetWindowSize(window, pWidth, pHeight)

      // Get the resolution of the primary monitor
      val vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor)

      // Center the window
      glfwSetWindowPos(window, (vidmode.width - pWidth.get(0)) / 2, (vidmode.height - pHeight.get(0)) / 2)
      stack.pop()
    } // the stack frame is popped automatically

    glfwMakeContextCurrent(window)
    // Enable v-sync
    glfwSwapInterval(if (vsync) 1 else 0)

    glfwShowWindow(window)
    window
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

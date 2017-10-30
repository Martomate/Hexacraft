package hexagon

import java.io.File

import hexagon.block.{Block, BlockLoader}
import hexagon.gui.menu.main.MainMenu
import hexagon.renderer.VAO
import hexagon.resource.{Resource, Shader}
import hexagon.scene.Scene
import hexagon.util.OSUtils
import org.joml.{Vector2d, Vector2dc, Vector2f, Vector2i}
import org.lwjgl.glfw.GLFW._
import org.lwjgl.glfw.{Callbacks, GLFW, GLFWErrorCallback}
import org.lwjgl.opengl.{GL, GL11}
import org.lwjgl.system.{Configuration, MemoryStack, MemoryUtil}

import scala.collection.mutable.ArrayBuffer

object Main {
  {
    var file = new File("lib/natives")
    if (!file.exists) file = new File(OSUtils.nativesPath)
    Configuration.LIBRARY_PATH.set(file.getAbsolutePath)
  }

  val saveFolder: File = new File(OSUtils.appdataPath, ".hexacraft")

  val windowSize = new Vector2i(960, 540)
  private val prevWindowSize = new Vector2i()
  private val prevWindowPos = new Vector2i()

  val window: Long = initWindow()
  private var fullscreen = false
  private var vsync = false

  private val _mousePos = new Vector2d()
  private val _mouseMoved = new Vector2d()

  private val doublePtrX = MemoryUtil.memAllocDouble(1)
  private val doublePtrY = MemoryUtil.memAllocDouble(1)
  private val intPtrX = MemoryUtil.memAllocInt(1)
  private val intPtrY = MemoryUtil.memAllocInt(1)
  def mousePos: Vector2dc = _mousePos.toImmutable
  def mouseMoved: Vector2dc = _mouseMoved.toImmutable
  def normalizedMousePos: Vector2f = new Vector2f((mousePos.x / windowSize.x * 2 - 1).toFloat, (mousePos.y / windowSize.y * 2 - 1).toFloat)

  private val sceneList = ArrayBuffer.empty[Scene]
  def pushScene(scene: Scene): Unit = {
    if (scene != null) {
      sceneList += scene
    }
  }
  def popScene(): Unit = {
    if (sceneList.nonEmpty) {
      val index = sceneList.size - 1
      sceneList(index).unload()
      sceneList.remove(index)
    }
  }

  def popScenesUntil(predicate: Scene => Boolean): Unit = {
    while (sceneList.nonEmpty && !predicate(sceneList.last)) popScene()
  }

  def popScenesUntilMainMenu(): Unit = {
    popScenesUntil(_.isInstanceOf[MainMenu])
  }
  
  def updateMousePos(): Unit = {
    glfwGetCursorPos(window, doublePtrX, doublePtrY)
    _mousePos.set(doublePtrX.get(0), windowSize.y - doublePtrY.get(0))
  }

  def loop(): Unit = {
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
          
          if (frames > 80) {
            if (!vsync) {
              vsync = true
              glfwSwapInterval(1)
            }
          } else if (frames < 50) {
            if (vsync) {
              vsync = false
              glfwSwapInterval(0)
            }
          }
          
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
        val debugInfo = (
            sceneList.map(_.windowTitle) :+ 
            (fps + " fps   ms: " + ((if (msTime < 10) "0" else "") + msTime)) :+ 
            (if (vsync) "vsync" else "")
        ).filter(!_.isEmpty).mkString("   |   ")
        
        glfwSetWindowTitle(window, "Hexacraft   |   " + debugInfo)
      }
      // Poll for window events. The key callback will only be
      // invoked during this call.
      glfwPollEvents()
    }
  }

  private def render(): Unit = {
    sceneList.foreach(_.render())

    VAO.unbindVAO()
  }

  private def tick(): Unit = {
    glfwGetCursorPos(window, doublePtrX, doublePtrY)
    val oldMouseX = _mousePos.x
    val oldMouseY = _mousePos.y
    _mousePos.set(doublePtrX.get(0), windowSize.y - doublePtrY.get(0))
    _mouseMoved.set(_mousePos.x - oldMouseX, _mousePos.y - oldMouseY)
    
    sceneList.foreach(_.tick()) // TODO: should maybe be reversed
  }

  def run(): Unit = {
    initGL()
    Shader.init()
    BlockLoader.init()// this loads it to memory
    Block.init()
    pushScene(new MainMenu)
    updateMousePos()
    loop()

    destroy()

    // Free the window callbacks and destroy the window
    Callbacks.glfwFreeCallbacks(window)
    glfwDestroyWindow(window)

    // Terminate GLFW and free the error callback
    glfwTerminate()
    glfwSetErrorCallback(null).free()
  }

  private def processKeys(window: Long, key: Int, scancode: Int, action: Int, mods: Int): Unit = { // action: 0 = release, 1 = press, 2 = repeat
    if (key == GLFW.GLFW_KEY_F3 && action == GLFW.GLFW_RELEASE) {
      Resource.reloadAllResources()
      sceneList.foreach(_.onReloadedResources())
    }
    if (key == GLFW.GLFW_KEY_F11 && action == GLFW.GLFW_RELEASE) {
      val monitor = glfwGetPrimaryMonitor()
      val mode = glfwGetVideoMode(monitor)
      
      if (!fullscreen) {
        prevWindowSize.set(windowSize)
        glfwGetWindowPos(window, intPtrX, intPtrY)
        prevWindowPos.set(intPtrX.get(0), intPtrY.get(0))
        
        
        _mousePos.add(prevWindowPos.x, mode.height - prevWindowSize.y - prevWindowPos.y)
        
        val cursorHidden = glfwGetInputMode(window, GLFW_CURSOR) == GLFW_CURSOR_DISABLED
        if (cursorHidden) glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_HIDDEN)
        glfwSetWindowMonitor(window, monitor, 0, 0, mode.width(), mode.height(), mode.refreshRate())
        updateMousePos()
        if (cursorHidden) glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED)
        
        fullscreen = true
      } else {
        _mousePos.sub(prevWindowPos.x, mode.height - prevWindowSize.y - prevWindowPos.y)
        
        val cursorHidden = glfwGetInputMode(window, GLFW_CURSOR) == GLFW_CURSOR_DISABLED
        if (cursorHidden) glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_HIDDEN)
        glfwSetWindowMonitor(window, 0, prevWindowPos.x, prevWindowPos.y, prevWindowSize.x, prevWindowSize.y, GLFW_DONT_CARE)
        updateMousePos()
        if (cursorHidden) glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED)
        
        fullscreen = false
      }
    }
    sceneList.reverseIterator.forall(!_.processKeys(key, scancode, action, mods))
  }

  private def processChar(window: Long, character: Int): Unit = {
    sceneList.reverseIterator.forall(!_.processChar(character))
  }

  private def processMouseButtons(window: Long, button: Int, action: Int, mods: Int): Unit = { // mods: 1 = Shift, 2 = Ctrl, 4 = Alt. These are combined with |
    sceneList.reverseIterator.forall(!_.processMouseButtons(button, action, mods))
  }

  private def resizeWindow(width: Int, height: Int): Unit = {
    if (width > 0 && height > 0) {
      if (width != windowSize.x || height != windowSize.y) {
        GL11.glViewport(0, 0, width, height)
  
        sceneList.foreach(_.windowResized(width, height))
      }
      windowSize.set(width, height)
    }
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

    val window = glfwCreateWindow(windowSize.x, windowSize.y, "Hexacraft", 0, 0)
    if (window == 0) throw new RuntimeException("Failed to create the GLFW window")

    // Setup a key callback. It will be called every time a key is pressed, repeated or released.
    glfwSetKeyCallback(window, processKeys)
    glfwSetCharCallback(window, processChar)
    glfwSetMouseButtonCallback(window, processMouseButtons)
    glfwSetWindowSizeCallback(window, (window, width, height) => resizeWindow(width, height))

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

//  Display.create(new PixelFormat.withSamples(2).withDepthBits(24), new ContextAttribs(3, 3))//samples = 2 or 4

//  GL11.glClearColor(0.1f, 0.2f, 0.7f, 1)
//  GL11.glEnable(GL13.GL_MULTISAMPLE)
    GL11.glEnable(GL11.GL_DEPTH_TEST)
    GL11.glDepthFunc(GL11.GL_LEQUAL)
    GL11.glEnable(GL11.GL_CULL_FACE)
  }

  def tryQuit(): Unit = {
    glfwSetWindowShouldClose(window, true)
  }

  def destroy(): Unit = {
    while (sceneList.nonEmpty) popScene()

    Resource.freeAllResources()
  }
  
  def main(args: Array[String]): Unit = {
    run()
  }
}

package com.martomate.hexacraft.main

import com.martomate.hexacraft.*
import com.martomate.hexacraft.GameKeyboard
import com.martomate.hexacraft.gui.{Event, Scene, SceneStack, WindowExtras, WindowScenes}
import com.martomate.hexacraft.gui.comp.GUITransformation
import com.martomate.hexacraft.menu.MainMenu
import com.martomate.hexacraft.renderer.{Shader, VAO}
import com.martomate.hexacraft.util.{AsyncFileIO, OpenGL, Resource}
import com.martomate.hexacraft.util.os.OSUtils
import com.martomate.hexacraft.world.World
import com.martomate.hexacraft.world.block.{BlockLoader, Blocks}

import java.io.File
import org.joml.{Vector2i, Vector2ic}
import org.lwjgl.glfw.{Callbacks, GLFWErrorCallback}
import org.lwjgl.glfw.GLFW.*

class MainWindow(isDebug: Boolean) extends GameWindow with WindowScenes with WindowExtras:
  val saveFolder: File = new File(OSUtils.appdataPath, ".hexacraft")

  private val _windowSize = new Vector2i(960, 540)
  def windowSize: Vector2ic = _windowSize

  private val _framebufferSize = new Vector2i(0, 0) // Initialized in initWindow
  def framebufferSize: Vector2ic = _framebufferSize

  private val glfwHelper = new GlfwHelper()
  private val callbackHandler = new CallbackHandler()

  private val vsyncManager = new VsyncManager(50, 80, onUpdateVsync)
  private val window: Long = initWindow()

  private val fullscreenManager = new FullscreenManager(window, glfwHelper)

  private val mouse: RealGameMouse = new RealGameMouse
  private val keyboard: GameKeyboard = new GameKeyboard.GlfwKeyboard(key => glfwGetKey(window, key) == GLFW_PRESS)

  private val scenes: SceneStack = new SceneStack
  def popScene(): Unit = scenes.popScene()
  def pushScene(scene: Scene): Unit = scenes.pushScene(scene)
  def popScenesUntil(predicate: Scene => Boolean): Unit = scenes.popScenesUntil(predicate)

  override def setCursorLayout(cursorLayout: Int): Unit =
    glfwSetInputMode(window, GLFW_CURSOR, cursorLayout)

  def resetMousePos(): Unit =
    val (cx, cy) = glfwHelper.getCursorPos(window)
    mouse.skipNextMouseMovedUpdate()
    mouse.moveTo(cx, _windowSize.y - cy)

  private def loop(): Unit =
    var prevTime = System.nanoTime
    var ticks, frames, fps, titleTicker = 0

    while !glfwWindowShouldClose(window)
    do
      val currentTime = System.nanoTime
      val delta = ((currentTime - prevTime) * 1e-9 * 60).toInt
      val realPrevTime = currentTime

      for (_ <- 0 until delta)
        tick()
        ticks += 1
        titleTicker += 1
        if ticks % 60 == 0
        then
          fps = frames
          vsyncManager.handleVsync(fps)
          World.shouldChillChunkLoader = fps < 20
          frames = 0
        prevTime += 1e9.toLong / 60

      OpenGL.glClear(OpenGL.ClearMask.colorBuffer | OpenGL.ClearMask.depthBuffer)
      render()

      OpenGL.glGetError() match
        case Some(error) => println("OpenGL error: " + error)
        case None        =>

      frames += 1

      val realDeltaTime = System.nanoTime - realPrevTime
      val msTime = (realDeltaTime * 1e-6).toInt

      glfwSwapBuffers(window)

      if titleTicker > 10
      then
        titleTicker = 0
        val msString = (if msTime < 10 then "0" else "") + msTime
        val vsyncStr = if vsyncManager.isVsync then "vsync" else ""
        val titleParts =
          "Hexacraft" +: scenes.map(_.windowTitle) :+ s"$fps fps   ms: $msString" :+ vsyncStr
        val windowTitle = titleParts.filter(_.nonEmpty).mkString("   |   ")

        glfwSetWindowTitle(window, windowTitle)

      // Poll for window events. The callbacks will (on most systems) only be invoked during this call.
      glfwPollEvents()

      callbackHandler.handle(processCallbackEvent)

  private def processCallbackEvent(event: CallbackEvent): Unit = event match
    case CallbackEvent.KeyPressed(window, key, scancode, action, mods) =>
      val keyAction = Event.KeyAction.fromGlfw(action)
      val keyMods = Event.KeyMods.fromGlfw(mods)

      val keyIsPressed = keyAction == Event.KeyAction.Press

      if keyIsPressed
      then
        if key == GLFW_KEY_R && glfwGetKey(window, GLFW_KEY_F3) == GLFW_PRESS
        then
          Resource.reloadAllResources()
          scenes.foreach(_.onReloadedResources())
          println("Reloaded resources")

        if key == GLFW_KEY_F11
        then setFullscreen()

      scenes.reverseIterator.exists(_.handleEvent(Event.KeyEvent(key, scancode, keyAction, keyMods)))
    case CallbackEvent.CharTyped(_, character) =>
      scenes.reverseIterator.exists(_.handleEvent(Event.CharEvent(character)))
    case CallbackEvent.MouseClicked(_, button, action, mods) =>
      val mouseButton = Event.MouseButton.fromGlfw(button)
      val mouseAction = Event.MouseAction.fromGlfw(action)
      val keyMods = Event.KeyMods.fromGlfw(mods)

      val normalizedMousePos = mouse.heightNormalizedPos(windowSize)
      val mousePos = (normalizedMousePos.x, normalizedMousePos.y)
      scenes.reverseIterator.exists(
        _.handleEvent(Event.MouseClickEvent(mouseButton, mouseAction, keyMods, mousePos))
      )
    case CallbackEvent.MouseScrolled(_, xOff, yOff) =>
      scenes.reverseIterator.exists(_.handleEvent(Event.ScrollEvent(xOff.toFloat, yOff.toFloat)))
    case CallbackEvent.WindowResized(_, w, h) =>
      if w > 0 && h > 0
      then
        if w != _windowSize.x || h != _windowSize.y
        then scenes.foreach(_.windowResized(w, h))

        _windowSize.set(w, h)
        resetMousePos()
        mouse.skipNextMouseMovedUpdate()

      Shader.foreach(_.setUniform2f("windowSize", _windowSize.x.toFloat, _windowSize.y.toFloat))
    case CallbackEvent.FramebufferResized(_, w, h) =>
      if w > 0 && h > 0
      then
        if w != _framebufferSize.x || h != _framebufferSize.y
        then
          OpenGL.glViewport(0, 0, w, h)
          scenes.foreach(_.framebufferResized(w, h))

        _framebufferSize.set(w, h)

  private def handleDebugEvent(debugMessage: OpenGL.DebugMessage): Unit =
    val OpenGL.DebugMessage(source, debugType, _, severity, message, _) = debugMessage
    val messageStr = s"[${severity.toString}] [${debugType.toString}] [${source.toString}] - $message"
    System.err.println(s"OpenGL debug: $messageStr")

  private def onUpdateVsync(vsync: Boolean): Unit = glfwSwapInterval(if vsync then 1 else 0)

  private def render(): Unit =
    def render(idx: Int): Unit =
      if idx > 0 && !scenes(idx).isOpaque
      then render(idx - 1)
      scenes(idx).render(GUITransformation(0, 0))

    render(scenes.size - 1)
    VAO.unbindVAO()

  private def tick(): Unit =
    val (cx, cy) = glfwHelper.getCursorPos(window)
    mouse.moveTo(cx, _windowSize.y - cy)

    scenes.foreach(_.tick()) // TODO: should maybe be reversed

  def run(): Unit =
    initGL()

    try
      given GameWindow = this
      given WindowExtras = this
      given WindowScenes = this
      given GameMouse = mouse
      given GameKeyboard = keyboard

      Shader.init()
      given BlockLoader = BlockLoader.instance // this loads it to memory
      given Blocks = new Blocks
      pushScene(new MainMenu(saveFolder, tryQuit))
      resetMousePos()
      Shader.foreach(_.setUniform2f("windowSize", _windowSize.x.toFloat, _windowSize.y.toFloat))
      loop()
    finally
      destroy()

      // Free the window callbacks and destroy the window
      Callbacks.glfwFreeCallbacks(window)
      glfwDestroyWindow(window)

      // Terminate GLFW and free the error callback
      glfwTerminate()
      glfwSetErrorCallback(null).free()

  private def setFullscreen(): Unit =
    fullscreenManager.toggleFullscreen()
    mouse.skipNextMouseMovedUpdate()

  private def initWindow(): Long =
    // Setup an error callback. The default implementation
    // will print the error message in System.err.
    GLFWErrorCallback.createPrint(System.err).set

    // Initialize GLFW. Most GLFW functions will not work before doing this.
    if !glfwInit()
    then throw new IllegalStateException("Unable to initialize GLFW")

    configureGlfw()

    val window = glfwCreateWindow(_windowSize.x, _windowSize.y, "Hexacraft", 0, 0)
    if window == 0
    then throw new RuntimeException("Failed to create the GLFW window")

    val (fw, fh) = glfwHelper.getFramebufferSize(window)
    _framebufferSize.set(fw, fh)

    setupCallbacks(window)

    centerWindow(window)

    glfwMakeContextCurrent(window)
    // Enable v-sync
    glfwSwapInterval(if vsyncManager.isVsync then 1 else 0)

    glfwShowWindow(window)
    window

  private def configureGlfw(): Unit =
    glfwDefaultWindowHints() // optional, the current window hints are already the default
    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
    glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE)

    // Minimum required OpenGL version is 3.3 (could be bumped to 4.1 in the future if needed)
    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)

    glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE)
    glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
    glfwWindowHint(GLFW_SAMPLES, 1)

    if isDebug
    then glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE)

  private def setupCallbacks(window: Long): Unit =
    callbackHandler.addKeyCallback(window)
    callbackHandler.addCharCallback(window)
    callbackHandler.addMouseButtonCallback(window)
    callbackHandler.addWindowSizeCallback(window)
    callbackHandler.addScrollCallback(window)
    callbackHandler.addFramebufferSizeCallback(window)

  private def centerWindow(window: Long): Unit =
    val (windowWidth, windowHeight) = glfwHelper.getWindowSize(window)

    val mode = glfwGetVideoMode(glfwGetPrimaryMonitor())

    glfwSetWindowPos(window, (mode.width - windowWidth) / 2, (mode.height - windowHeight) / 2)

  private def initGL(): Unit =
    OpenGL.createCapabilities()

//  OpenGL.glEnable(OpenGL.State.MultiSample)
    OpenGL.glEnable(OpenGL.State.DepthTest)
    OpenGL.glDepthFunc(OpenGL.DepthFunc.LessThanOrEqual)
    OpenGL.glEnable(OpenGL.State.CullFace)

    if isDebug && OpenGL.hasDebugExtension
    then
      OpenGL.glEnable(OpenGL.State.DebugOutput)
      OpenGL.glDebugMessageCallback(handleDebugEvent, 0L)

  private def tryQuit(): Unit =
    glfwSetWindowShouldClose(window, true)

  private def destroy(): Unit =
    popScenesUntil(_ => false)

    Resource.freeAllResources()
    AsyncFileIO.unload()

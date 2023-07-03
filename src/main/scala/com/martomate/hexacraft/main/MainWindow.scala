package com.martomate.hexacraft.main

import com.martomate.hexacraft.*
import com.martomate.hexacraft.GameKeyboard
import com.martomate.hexacraft.gui.{Event, Scene, SceneStack, WindowExtras, WindowScenes}
import com.martomate.hexacraft.gui.comp.GUITransformation
import com.martomate.hexacraft.infra.gpu.OpenGL
import com.martomate.hexacraft.infra.window.{
  CallbackEvent,
  CursorMode,
  KeyAction,
  KeyboardKey,
  Window,
  WindowSettings,
  WindowSystem
}
import com.martomate.hexacraft.menu.MainMenu
import com.martomate.hexacraft.renderer.{Shader, VAO}
import com.martomate.hexacraft.util.{AsyncFileIO, Resource, Result}
import com.martomate.hexacraft.util.os.OSUtils
import com.martomate.hexacraft.world.World
import com.martomate.hexacraft.world.block.{BlockLoader, Blocks}

import java.io.File
import org.joml.{Vector2i, Vector2ic}
import scala.collection.mutable

class MainWindow(isDebug: Boolean) extends GameWindow with WindowScenes with WindowExtras:
  val saveFolder: File = new File(OSUtils.appdataPath, ".hexacraft")

  private val multiplayerEnabled = isDebug

  private val _windowSize = new Vector2i(960, 540)
  def windowSize: Vector2ic = _windowSize

  private val _framebufferSize = new Vector2i(0, 0) // Initialized in initWindow
  def framebufferSize: Vector2ic = _framebufferSize

  private val callbackQueue = mutable.Queue.empty[CallbackEvent]
  private val vsyncManager = new VsyncManager(50, 80, onUpdateVsync)

  private val windowSystem = createWindowSystem()
  private val window: Window = initWindow(_windowSize.x, _windowSize.y)

  private val fullscreenManager = new FullscreenManager(window, windowSystem)

  private val mouse: RealGameMouse = new RealGameMouse
  private val keyboard: GameKeyboard = new GameKeyboard.GlfwKeyboard(window)

  private val scenes: SceneStack = new SceneStack
  def popScene(): Unit = scenes.popScene()
  def pushScene(scene: Scene): Unit = scenes.pushScene(scene)
  def popScenesUntil(predicate: Scene => Boolean): Unit = scenes.popScenesUntil(predicate)

  override def setCursorMode(cursorMode: CursorMode): Unit =
    window.setCursorMode(cursorMode)

  def resetMousePos(): Unit =
    val (cx, cy) = window.cursorPosition
    mouse.skipNextMouseMovedUpdate()
    mouse.moveTo(cx, _windowSize.y - cy)

  private def loop(): Unit =
    var prevTime = System.nanoTime
    var ticks, frames, fps, titleTicker = 0

    while !window.shouldClose
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

      window.swapBuffers()

      if titleTicker > 10
      then
        titleTicker = 0
        val msString = (if msTime < 10 then "0" else "") + msTime
        val vsyncStr = if vsyncManager.isVsync then "vsync" else ""
        val titleParts =
          "Hexacraft" +: scenes.map(_.windowTitle) :+ s"$fps fps   ms: $msString" :+ vsyncStr
        val windowTitle = titleParts.filter(_.nonEmpty).mkString("   |   ")

        window.setTitle(windowTitle)

      // Put occurred events into event queue
      windowSystem.runEventCallbacks()

      // Process events in event queue
      while callbackQueue.nonEmpty
      do processCallbackEvent(callbackQueue.dequeue())

  private def processCallbackEvent(event: CallbackEvent): Unit = event match
    case CallbackEvent.KeyPressed(window, key, scancode, action, mods) =>
      val keyIsPressed = action == KeyAction.Press

      if keyIsPressed
      then
        if key == KeyboardKey.Letter('R') && window.isKeyPressed(KeyboardKey.Function(3))
        then
          Resource.reloadAllResources()
          scenes.foreach(_.onReloadedResources())
          println("Reloaded resources")

        if key == KeyboardKey.Function(11)
        then setFullscreen()

      scenes.reverseIterator.exists(_.handleEvent(Event.KeyEvent(key, scancode, action, mods)))

    case CallbackEvent.CharTyped(_, character) =>
      scenes.reverseIterator.exists(_.handleEvent(Event.CharEvent(character)))

    case CallbackEvent.MouseClicked(_, button, action, mods) =>
      val normalizedMousePos = mouse.heightNormalizedPos(windowSize)
      val mousePos = (normalizedMousePos.x, normalizedMousePos.y)
      scenes.reverseIterator.exists(
        _.handleEvent(Event.MouseClickEvent(button, action, mods, mousePos))
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

  private def handleDebugEvent(debugMessage: OpenGL.Debug.Message): Unit =
    val OpenGL.Debug.Message(source, debugType, _, severity, message, _) = debugMessage
    val messageStr = s"[${severity.toString}] [${debugType.toString}] [${source.toString}] - $message"
    System.err.println(s"OpenGL debug: $messageStr")

  private def onUpdateVsync(vsync: Boolean): Unit = windowSystem.setVsync(vsync)

  private def render(): Unit =
    given GameWindow = this

    def render(idx: Int): Unit =
      if idx > 0 && !scenes(idx).isOpaque
      then render(idx - 1)
      scenes(idx).render(GUITransformation(0, 0))

    render(scenes.size - 1)
    VAO.unbindVAO()

  private def tick(): Unit =
    val (cx, cy) = window.cursorPosition
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
      pushScene(new MainMenu(saveFolder, tryQuit, multiplayerEnabled))
      resetMousePos()
      Shader.foreach(_.setUniform2f("windowSize", _windowSize.x.toFloat, _windowSize.y.toFloat))
      loop()
    finally
      destroy()

      window.close()
      windowSystem.shutdown()

  private def setFullscreen(): Unit =
    fullscreenManager.toggleFullscreen()
    mouse.skipNextMouseMovedUpdate()

  private def createWindowSystem(): WindowSystem =
    val windowSystem = WindowSystem.create()
    windowSystem.setErrorCallback(e => System.err.println(s"[LWJGL] ${e.reason} error: ${e.description}"))
    windowSystem.initialize()
    windowSystem

  private def initWindow(width: Int, height: Int): Window =
    val openglSettings = WindowSettings.Opengl(3, 3, isDebug)
    val windowSettings = WindowSettings(width, height, "Hexacraft", openglSettings, resizable = true, samples = 1)

    val window = Result
      .fromOption(windowSystem.createWindow(windowSettings))
      .mapErr(_ => new RuntimeException("Failed to create the window"))
      .unwrap()

    val (fw, fh) = window.framebufferSize
    _framebufferSize.set(fw, fh)

    window.setKeyCallback(callbackQueue.enqueue)
    window.setCharCallback(callbackQueue.enqueue)
    window.setMouseButtonCallback(callbackQueue.enqueue)
    window.setWindowSizeCallback(callbackQueue.enqueue)
    window.setScrollCallback(callbackQueue.enqueue)
    window.setFramebufferSizeCallback(callbackQueue.enqueue)

    centerWindow(window)

    window.makeContextCurrent()
    windowSystem.setVsync(vsyncManager.isVsync)
    window.show()
    window

  private def centerWindow(window: Window): Unit =
    val (windowWidth, windowHeight) = window.size
    val mode = windowSystem.primaryMonitor.videoMode
    window.moveTo((mode.width - windowWidth) / 2, (mode.height - windowHeight) / 2)

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

  private def tryQuit(): Unit = window.requestClose()

  private def destroy(): Unit =
    popScenesUntil(_ => false)

    Resource.freeAllResources()
    AsyncFileIO.unload()

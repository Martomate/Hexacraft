package hexacraft.main

import hexacraft.game.GameKeyboard
import hexacraft.gui.*
import hexacraft.infra.audio.AudioSystem
import hexacraft.infra.fs.FileSystem
import hexacraft.infra.gpu.OpenGL
import hexacraft.infra.window.*
import hexacraft.renderer.VAO
import hexacraft.server.ServerWorld
import hexacraft.util.{Resource, Result}

import org.joml.{Vector2f, Vector2i}

import java.io.File
import scala.collection.mutable

class MainWindow(
    isDebug: Boolean,
    saveFolder: File,
    fs: FileSystem,
    audioSystem: AudioSystem,
    windowSystem: WindowSystem
) extends GameWindow {
  private val multiplayerEnabled = isDebug

  private var _windowSize = WindowSize(Vector2i(960, 540), Vector2i(0, 0)) // Initialized in initWindow

  def windowSize: WindowSize = _windowSize

  private val callbackQueue = mutable.Queue.empty[CallbackEvent]
  private val vsyncManager = new VsyncManager(50, 80, onUpdateVsync)

  setupWindowSystem(windowSystem)
  private val window: Window = initWindow(_windowSize.logicalSize.x, _windowSize.logicalSize.y)

  private val fullscreenManager = new FullscreenManager(window, windowSystem)

  private val mouse: GameMouse = new GameMouse
  private val keyboard: GameKeyboard = new GameKeyboard.GlfwKeyboard(window)

  private var scene: Option[Scene] = None
  private var nextScene: Option[SceneRoute] = Some(SceneRoute.Main)

  private val router = makeSceneRouter()

  override def setCursorMode(cursorMode: CursorMode): Unit = {
    window.setCursorMode(cursorMode)
    resetMousePos()
  }

  private def resetMousePos(): Unit = {
    val (cx, cy) = window.cursorPosition
    mouse.skipNextMouseMovedUpdate()
    mouse.moveTo(cx, _windowSize.logicalSize.y - cy)
    mouse.skipNextMouseMovedUpdate()
  }

  def setNextScene(scene: SceneRoute): Unit = {
    this.nextScene = Some(scene)
  }

  private def switchSceneIfNeeded(): Unit = {
    if nextScene.isDefined then {
      val (s, rx) = router.route(nextScene.get)
      nextScene = None

      rx.onEvent {
        case MainRouter.Event.ChangeScene(newRoute) =>
          setNextScene(newRoute)
        case MainRouter.Event.QuitRequested =>
          window.requestClose()
      }

      setScene(s)
    }
  }

  private def loop(): Unit = {
    var prevTime = System.nanoTime
    var ticks, frames, fps, titleTicker = 0

    while !window.shouldClose do {
      val currentTime = System.nanoTime
      val delta = ((currentTime - prevTime) * 1e-9 * 60).toInt
      val realPrevTime = currentTime

      switchSceneIfNeeded()

      var tIdx = 0
      while tIdx < delta do {
        tick()
        ticks += 1
        titleTicker += 1
        if ticks % 60 == 0 then {
          fps = frames
          vsyncManager.handleVsync(fps)
          ServerWorld.shouldChillChunkLoader = fps < 20
          frames = 0
        }
        prevTime += 1e9.toLong / 60
        tIdx += 1
      }

      OpenGL.glClear(OpenGL.ClearMask.colorBuffer | OpenGL.ClearMask.depthBuffer)
      render()

      OpenGL.glGetError() match {
        case Some(error) => println("OpenGL error: " + error)
        case None        =>
      }

      frames += 1

      val realDeltaTime = System.nanoTime - realPrevTime
      val msTime = (realDeltaTime * 1e-6).toInt

      window.swapBuffers()

      if titleTicker > 10
      then {
        titleTicker = 0
        window.setTitle(WindowTitle(fps, msTime, vsyncManager.isVsync).format)
      }

      // Put occurred events into event queue
      windowSystem.runEventCallbacks()

      // Process events in event queue
      while callbackQueue.nonEmpty do {
        processCallbackEvent(callbackQueue.dequeue())
      }
    }
  }

  private def processCallbackEvent(event: CallbackEvent): Unit = event match {
    case CallbackEvent.KeyPressed(window, key, scancode, action, mods) =>
      val keyIsPressed = action == KeyAction.Press

      if keyIsPressed
      then {
        if key == KeyboardKey.Function(11)
        then {
          toggleFullscreen()
        }
      }

      if scene.isDefined then {
        scene.get.handleEvent(Event.KeyEvent(key, scancode, action, mods))
      }

    case CallbackEvent.CharTyped(_, character) =>
      if scene.isDefined then {
        scene.get.handleEvent(Event.CharEvent(character))
      }

    case CallbackEvent.MouseClicked(_, button, action, mods) =>
      val normalizedMousePos = mouse.currentPos.heightNormalizedPos(windowSize.logicalSize)
      val mousePos = (normalizedMousePos.x, normalizedMousePos.y)
      if scene.isDefined then {
        scene.get.handleEvent(Event.MouseClickEvent(button, action, mods, mousePos))
      }

    case CallbackEvent.MouseScrolled(_, xOff, yOff) =>
      val normalizedMousePos = mouse.currentPos.heightNormalizedPos(windowSize.logicalSize)
      val mousePos = (normalizedMousePos.x, normalizedMousePos.y)
      if scene.isDefined then {
        scene.get.handleEvent(Event.ScrollEvent(xOff.toFloat, yOff.toFloat, mousePos))
      }

    case CallbackEvent.WindowResized(_, w, h) =>
      if w > 0 && h > 0
      then {
        if w != _windowSize.logicalSize.x || h != _windowSize.logicalSize.y
        then {
          if scene.isDefined then {
            scene.get.windowResized(w, h)
          }
        }

        _windowSize = WindowSize(Vector2i(w, h), _windowSize.physicalSize)
        resetMousePos()
      }

    case CallbackEvent.WindowFocusChanged(_, focused) =>
      if scene.isDefined then {
        scene.get.windowFocusChanged(focused)
      }

    case CallbackEvent.FrameBufferResized(_, w, h) =>
      if w > 0 && h > 0
      then {
        if w != _windowSize.physicalSize.x || h != _windowSize.physicalSize.y
        then {
          OpenGL.glViewport(0, 0, w, h)
          if scene.isDefined then {
            scene.get.frameBufferResized(w, h)
          }
        }

        _windowSize = WindowSize(_windowSize.logicalSize, Vector2i(w, h))
      }
  }

  private def handleDebugEvent(debugMessage: OpenGL.Debug.Message): Unit = {
    val OpenGL.Debug.Message(source, debugType, _, severity, message, _) = debugMessage
    val messageStr = s"[${severity.toString}] [${debugType.toString}] [${source.toString}] - $message"
    System.err.println(s"OpenGL debug: $messageStr")
  }

  private def onUpdateVsync(vsync: Boolean): Unit = {
    windowSystem.setVsync(vsync)
  }

  private def render(): Unit = {
    if scene.isDefined then {
      scene.get.render(
        RenderContext(
          this._windowSize.logicalAspectRatio,
          this._windowSize.physicalSize,
          mouse.currentPos.heightNormalizedPos(this.windowSize.logicalSize),
          Vector2f(0, 0)
        )
      )
    }
    VAO.unbindVAO()
  }

  private def tick(): Unit = {
    val (cx, cy) = window.cursorPosition
    mouse.moveTo(cx, _windowSize.logicalSize.y - cy)

    if scene.isDefined then {
      scene.get.tick(
        TickContext(
          windowSize = _windowSize,
          currentMousePosition = mouse.currentPos,
          previousMousePosition = mouse.previousPos
        )
      )
    }
  }

  private def setScene(newScene: Scene): Unit = {
    if scene.isDefined then {
      scene.get.unload()
    }
    scene = Some(newScene)
  }

  private def makeSceneRouter(): MainRouter = {
    MainRouter(saveFolder, multiplayerEnabled, fs, this, keyboard, audioSystem)
  }

  def run(): Unit = {
    initGL()
    audioSystem.init()

    try {
      resetMousePos()
      loop()
    } finally {
      destroy()

      window.close()
      windowSystem.shutdown()
    }
  }

  private def toggleFullscreen(): Unit = {
    fullscreenManager.toggleFullscreen()
    mouse.skipNextMouseMovedUpdate()
  }

  private def setupWindowSystem(windowSystem: WindowSystem): WindowSystem = {
    windowSystem.setErrorCallback(e => System.err.println(s"[LWJGL] ${e.reason} error: ${e.description}"))
    windowSystem.initialize()
    windowSystem
  }

  private def initWindow(width: Int, height: Int): Window = {
    val openglSettings = WindowSettings.Opengl(3, 3, isDebug)
    val windowSettings = WindowSettings(width, height, "Hexacraft", openglSettings, resizable = true, samples = 1)

    val window = Result
      .fromOption(windowSystem.createWindow(windowSettings))
      .mapErr(_ => new RuntimeException("Failed to create the window"))
      .unwrap()

    val (fw, fh) = window.framebufferSize
    _windowSize = WindowSize(_windowSize.logicalSize, Vector2i(fw, fh))

    window.setKeyCallback(callbackQueue.enqueue)
    window.setCharCallback(callbackQueue.enqueue)
    window.setMouseButtonCallback(callbackQueue.enqueue)
    window.setWindowSizeCallback(callbackQueue.enqueue)
    window.setWindowFocusCallback(callbackQueue.enqueue)
    window.setScrollCallback(callbackQueue.enqueue)
    window.setFrameBufferSizeCallback(callbackQueue.enqueue)

    centerWindow(window)

    window.makeContextCurrent()
    windowSystem.setVsync(vsyncManager.isVsync)
    window.show()
    window
  }

  private def centerWindow(window: Window): Unit = {
    val (windowWidth, windowHeight) = window.size
    val mode = windowSystem.primaryMonitor.videoMode
    window.moveTo((mode.width - windowWidth) / 2, (mode.height - windowHeight) / 2)
  }

  private def initGL(): Unit = {
    OpenGL.createCapabilities()

    //  OpenGL.glEnable(OpenGL.State.MultiSample)
    OpenGL.glEnable(OpenGL.State.DepthTest)
    OpenGL.glDepthFunc(OpenGL.DepthFunc.LessThanOrEqual)
    OpenGL.glBlendFunc(OpenGL.BlendFactor.SrcAlpha, OpenGL.BlendFactor.OneMinusSrcAlpha)
    OpenGL.glEnable(OpenGL.State.CullFace)

    if isDebug && OpenGL.hasDebugExtension then {
      OpenGL.glEnable(OpenGL.State.DebugOutput)
      OpenGL.glDebugMessageCallback(handleDebugEvent, 0L)
    }
  }

  private def destroy(): Unit = {
    if scene.isDefined then {
      scene.get.unload()
    }

    Resource.freeAllResources()
  }
}

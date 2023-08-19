package hexacraft.main

import hexacraft.game.{GameKeyboard, GameMouse, GameScene, GameWindow}
import hexacraft.gui.{Event, Scene, WindowExtras}
import hexacraft.infra.fs.FileSystem
import hexacraft.infra.gpu.OpenGL
import hexacraft.infra.window.*
import hexacraft.math.GzipAlgorithm
import hexacraft.menu.*
import hexacraft.nbt.Nbt
import hexacraft.util.Tracker
import hexacraft.world.settings.WorldSettings

import munit.FunSuite
import org.joml.{Vector2i, Vector2ic}

import java.nio.file.Path

class MainRouterTest extends FunSuite {
  override def beforeEach(context: BeforeEach): Unit = OpenGL._enterTestMode()

  given GameWindow = null
  given GameMouse = null
  given GameKeyboard = null
  given WindowExtras = null

  private val saveDirPath = Path.of("abc")

  def performSingleRoute(route: SceneRoute, fs: FileSystem = FileSystem.createNull())(using
      GameMouse,
      GameWindow,
      WindowExtras
  ): Scene =
    val tracker = Tracker.withStorage[MainRouter.Event]
    val router = new MainRouter(saveDirPath.toFile, false, fs)(tracker)

    router.route(route)

    assertEquals(tracker.events.size, 1)
    val scene = tracker.events.collectFirst:
      case MainRouter.Event.SceneChanged(s) => s

    assert(scene.isDefined)
    scene.get

  def performRouteAndSendEvents(route: SceneRoute, events: Seq[Event], fs: FileSystem)(using
      GameMouse,
      GameWindow,
      WindowExtras
  )(using munit.Location): Seq[MainRouter.Event] =
    val tracker = Tracker.withStorage[MainRouter.Event]
    val router = new MainRouter(saveDirPath.toFile, true, fs)(tracker)

    router.route(route)

    assertEquals(tracker.events.size, 1)
    val scene1 = tracker.events.head.asInstanceOf[MainRouter.Event.SceneChanged].newScene

    for e <- events do scene1.handleEvent(e)

    assertEquals(tracker.events.size, 2)
    tracker.events.drop(1)

  def performRouteAndClick(route: SceneRoute, clickAt: (Float, Float), fs: FileSystem = FileSystem.createNull())(using
      GameMouse,
      GameWindow,
      WindowExtras
  )(using munit.Location): Seq[MainRouter.Event] =
    val events = Seq(
      Event.MouseClickEvent(MouseButton.Left, MouseAction.Press, KeyMods.none, clickAt),
      Event.MouseClickEvent(MouseButton.Left, MouseAction.Release, KeyMods.none, clickAt)
    )

    performRouteAndSendEvents(route, events, fs)

  def assertSingleScene(events: Seq[MainRouter.Event], sceneIsOk: Scene => Boolean): Unit =
    val scene = events.collectFirst:
      case MainRouter.Event.SceneChanged(s) => s

    assert(scene.isDefined)
    assert(sceneIsOk(scene.get))

  def testMainMenu(): Unit = {
    test("Main routes to MainMenu") {
      val scene = performSingleRoute(SceneRoute.Main)
      assert(scene.isInstanceOf[MainMenu])
    }

    test("Main with click on Play routes to WorldChooserMenu") {
      val events = performRouteAndClick(SceneRoute.Main, (0, 0.2f))
      assertSingleScene(events, _.isInstanceOf[WorldChooserMenu])
    }

    test("Main with click on Multiplayer routes to MultiplayerMenu") {
      val events = performRouteAndClick(SceneRoute.Main, (0, -0.1f))
      assertSingleScene(events, _.isInstanceOf[MultiplayerMenu])
    }

    test("Main with click on Settings routes to SettingsMenu") {
      val events = performRouteAndClick(SceneRoute.Main, (0, -0.4f))
      assertSingleScene(events, _.isInstanceOf[SettingsMenu])
    }

    test("Main with click on Quit causes a QuitRequest") {
      val events = performRouteAndClick(SceneRoute.Main, (0, -0.8f))
      assertEquals(events, Seq(MainRouter.Event.QuitRequested))
    }
  }

  def testWorldChooserMenu(): Unit = {
    val windowWidth = 1920
    val windowHeight = 1080

    given GameWindow = new GameWindow:
      override def windowSize: Vector2ic = new Vector2i(windowWidth, windowHeight)
      override def framebufferSize: Vector2ic = new Vector2i(windowWidth, windowHeight)

    given WindowExtras = new WindowExtras:
      override def resetMousePos(): Unit = ()
      override def setCursorMode(cursorMode: CursorMode): Unit = ()

    test("WorldChooser routes to WorldChooserMenu") {
      val scene = performSingleRoute(SceneRoute.WorldChooser)
      assert(scene.isInstanceOf[WorldChooserMenu])
    }

    test("WorldChooser with click on Back to menu routes to MainMenu") {
      val events = performRouteAndClick(SceneRoute.WorldChooser, (-0.1f, -0.8f))
      assertSingleScene(events, _.isInstanceOf[MainMenu])
    }

    test("WorldChooser with click on New world routes to NewWorldMenu") {
      val events = performRouteAndClick(SceneRoute.WorldChooser, (0.1f, -0.8f))
      assertSingleScene(events, _.isInstanceOf[NewWorldMenu])
    }

    test("WorldChooser with click on one of the worlds routes to GameScene".ignore) {
      val fs = FileSystem.createNull(
        Map(
          saveDirPath.resolve("saves") -> Array(),
          saveDirPath.resolve("saves/world_1") -> Array(),
          saveDirPath.resolve("saves/world_1/world.dat") ->
            GzipAlgorithm.compress(Nbt.makeMap().toBinary())
        )
      )

      val events = performRouteAndClick(SceneRoute.WorldChooser, (0, 0.6f), fs)
      assertSingleScene(events, _.isInstanceOf[GameScene])
    }
  }

  def testNewWorldMenu(): Unit = {
    val windowWidth = 1920
    val windowHeight = 1080

    given GameWindow = new GameWindow:
      override def windowSize: Vector2ic = new Vector2i(windowWidth, windowHeight)
      override def framebufferSize: Vector2ic = new Vector2i(windowWidth, windowHeight)

    given WindowExtras = new WindowExtras:
      override def resetMousePos(): Unit = ()
      override def setCursorMode(cursorMode: CursorMode): Unit = ()

    test("NewWorld routes to NewWorldMenu") {
      val scene = performSingleRoute(SceneRoute.NewWorld)
      assert(scene.isInstanceOf[NewWorldMenu])
    }

    test("NewWorld with click on Cancel routes to WorldChooserMenu") {
      val events = performRouteAndClick(SceneRoute.NewWorld, (-0.1f, -0.8f))
      assertSingleScene(events, _.isInstanceOf[WorldChooserMenu])
    }

    test("NewWorld with click on Create world routes to GameScene".ignore) {
      val fs = FileSystem.createNull(
        Map(
          saveDirPath.resolve("saves") -> Array(),
          saveDirPath.resolve("saves/world_1") -> Array(),
          saveDirPath.resolve("saves/world_1/world.dat") ->
            GzipAlgorithm.compress(Nbt.makeMap().toBinary())
        )
      )

      val events = performRouteAndClick(SceneRoute.NewWorld, (0.1f, -0.8f))
      assertSingleScene(events, _.isInstanceOf[GameScene])
    }
  }

  def testMultiplayerMenu(): Unit = {
    test("Multiplayer routes to MultiplayerMenu") {
      val scene = performSingleRoute(SceneRoute.Multiplayer)
      assert(scene.isInstanceOf[MultiplayerMenu])
    }

    test("Multiplayer with click on Join routes to JoinWorldChooserMenu") {
      val events = performRouteAndClick(SceneRoute.Multiplayer, (0, 0.2f))
      assertSingleScene(events, _.isInstanceOf[JoinWorldChooserMenu])
    }

    test("Multiplayer with click on Host routes to HostWorldChooserMenu") {
      val events = performRouteAndClick(SceneRoute.Multiplayer, (0, -0.1f))
      assertSingleScene(events, _.isInstanceOf[HostWorldChooserMenu])
    }

    test("Multiplayer with click on Back routes to MainMenu") {
      val events = performRouteAndClick(SceneRoute.Multiplayer, (0, -0.8f))
      assertSingleScene(events, _.isInstanceOf[MainMenu])
    }
  }

  def testJoinWorldChooserMenu(): Unit = {
    test("JoinWorld routes to JoinWorldChooserMenu") {
      val scene = performSingleRoute(SceneRoute.JoinWorld)
      assert(scene.isInstanceOf[JoinWorldChooserMenu])
    }

    test("JoinWorld with click on Back routes to MultiplayerMenu") {
      val events = performRouteAndClick(SceneRoute.JoinWorld, (-0.1f, -0.8f))
      assertSingleScene(events, _.isInstanceOf[MultiplayerMenu])
    }
  }

  def testHostWorldChooserMenu(): Unit = {
    test("HostWorld routes to HostWorldChooserMenu") {
      val scene = performSingleRoute(SceneRoute.HostWorld)
      assert(scene.isInstanceOf[HostWorldChooserMenu])
    }

    test("HostWorld with click on Back routes to MultiplayerMenu") {
      val events = performRouteAndClick(SceneRoute.HostWorld, (-0.1f, -0.8f))
      assertSingleScene(events, _.isInstanceOf[MultiplayerMenu])
    }
  }

  def testSettingsMenu(): Unit = {
    test("Settings routes to SettingsMenu") {
      val scene = performSingleRoute(SceneRoute.Settings)
      assert(scene.isInstanceOf[SettingsMenu])
    }

    test("Settings with click on Back routes to MainMenu") {
      val events = performRouteAndClick(SceneRoute.Settings, (0, -0.4f))
      assertSingleScene(events, _.isInstanceOf[MainMenu])
    }
  }

  def testGameScene(): Unit = {
    given GameWindow = new GameWindow:
      override def windowSize: Vector2ic = new Vector2i(1920, 1080)
      override def framebufferSize: Vector2ic = new Vector2i(1920, 1080)

    given WindowExtras = new WindowExtras:
      override def resetMousePos(): Unit = ()
      override def setCursorMode(cursorMode: CursorMode): Unit = ()

    test("Game routes to GameScene".ignore) {
      val scene = performSingleRoute(SceneRoute.Game(saveDirPath.toFile, WorldSettings.none))
      assert(scene.isInstanceOf[GameScene])
    }

    test("Game with Escape key and click on Back to menu routes to MainMenu".ignore) {
      val clickAt = (0f, -0.4f)

      val events = performRouteAndSendEvents(
        SceneRoute.Game(saveDirPath.toFile, WorldSettings.none),
        Seq(
          Event.KeyEvent(KeyboardKey.Escape, 0, KeyAction.Press, KeyMods.none),
          Event.MouseClickEvent(MouseButton.Left, MouseAction.Press, KeyMods.none, clickAt),
          Event.MouseClickEvent(MouseButton.Left, MouseAction.Release, KeyMods.none, clickAt)
        ),
        FileSystem.createNull()
      )
      assertSingleScene(events, _.isInstanceOf[MainMenu])
    }
  }

  testMainMenu()
  testWorldChooserMenu()
  testNewWorldMenu()
  testMultiplayerMenu()
  testJoinWorldChooserMenu()
  testHostWorldChooserMenu()
  testSettingsMenu()
  testGameScene()
}

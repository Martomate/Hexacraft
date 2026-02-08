package hexacraft.main

import hexacraft.gui.{Event, Scene}
import hexacraft.infra.audio.AudioSystem
import hexacraft.infra.fs.FileSystem
import hexacraft.infra.gpu.OpenGL
import hexacraft.infra.window.*
import hexacraft.math.GzipAlgorithm
import hexacraft.nbt.Nbt
import hexacraft.util.Tracker

import munit.FunSuite

import java.nio.file.Path

class MainRouterTest extends FunSuite {
  override def beforeEach(context: BeforeEach): Unit = OpenGL._enterTestMode()

  private val saveDirPath = Path.of("abc")

  def performSingleRoute(route: SceneRoute, fs: FileSystem = FileSystem.createNull()): Scene = {
    val router = new MainRouter(saveDirPath.toFile, false, fs, null, AudioSystem.createNull())
    val (s, _) = router.route(route)
    s
  }

  def performRouteAndSendEvents(route: SceneRoute, events: Seq[Event], fs: FileSystem)(using
      munit.Location
  ): SceneRouter.Event = {
    val router = new MainRouter(saveDirPath.toFile, true, fs, null, AudioSystem.createNull())

    val (s, rx) = router.route(route)
    val tracker = Tracker.fromRx(rx)

    for e <- events do {
      s.handleEvent(e)
    }

    assertEquals(tracker.events.size, 1)
    tracker.events.head
  }

  def performRouteAndClick(route: SceneRoute, clickAt: (Float, Float), fs: FileSystem = FileSystem.createNull())(using
      munit.Location
  ): SceneRouter.Event = {
    val events = Seq(
      Event.MouseClickEvent(MouseButton.Left, MouseAction.Press, KeyMods.none, clickAt),
      Event.MouseClickEvent(MouseButton.Left, MouseAction.Release, KeyMods.none, clickAt)
    )

    performRouteAndSendEvents(route, events, fs)
  }

  def assertSingleScene(events: Seq[SceneRouter.Event], sceneIsOk: SceneRoute => Boolean): Unit = {
    val scene = events.collectFirst:
      case SceneRouter.Event.ChangeScene(s) => s

    assert(scene.isDefined)
    assert(sceneIsOk(scene.get))
  }

  def assertSceneChange(event: SceneRouter.Event, sceneRoute: SceneRoute): Unit = {
    assertEquals(event, SceneRouter.Event.ChangeScene(sceneRoute))
  }

  def testMainMenu(): Unit = {
    test("Main routes to MainMenu") {
      val scene = performSingleRoute(SceneRoute.Main)
      assert(scene.isInstanceOf[Menus.MainMenu])
    }

    test("Main with click on Play routes to WorldChooserMenu") {
      val event = performRouteAndClick(SceneRoute.Main, (0, 0.2f))
      assertSceneChange(event, SceneRoute.WorldChooser)
    }

    test("Main with click on Multiplayer routes to MultiplayerMenu") {
      val event = performRouteAndClick(SceneRoute.Main, (0, -0.1f))
      assertSceneChange(event, SceneRoute.Multiplayer)
    }

    test("Main with click on Settings routes to SettingsMenu") {
      val event = performRouteAndClick(SceneRoute.Main, (0, -0.4f))
      assertSceneChange(event, SceneRoute.Settings)
    }

    test("Main with click on Quit causes a QuitRequest") {
      val event = performRouteAndClick(SceneRoute.Main, (0, -0.8f))
      assertEquals(event, SceneRouter.Event.QuitRequested)
    }
  }

  def testWorldChooserMenu(): Unit = {
    test("WorldChooser routes to WorldChooserMenu") {
      val scene = performSingleRoute(SceneRoute.WorldChooser)
      assert(scene.isInstanceOf[Menus.WorldChooserMenu])
    }

    test("WorldChooser with click on Back to menu routes to MainMenu") {
      val event = performRouteAndClick(SceneRoute.WorldChooser, (-0.1f, -0.8f))
      assertSceneChange(event, SceneRoute.Main)
    }

    test("WorldChooser with click on New world routes to NewWorldMenu") {
      val event = performRouteAndClick(SceneRoute.WorldChooser, (0.1f, -0.8f))
      assertSceneChange(event, SceneRoute.NewWorld)
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

      val event = performRouteAndClick(SceneRoute.WorldChooser, (0, 0.6f), fs)
      assertSceneChange(event, SceneRoute.Game(saveDirPath.toFile, true, false, null))
    }
  }

  def testNewWorldMenu(): Unit = {
    test("NewWorld routes to NewWorldMenu") {
      val scene = performSingleRoute(SceneRoute.NewWorld)
      assert(scene.isInstanceOf[Menus.NewWorldMenu])
    }

    test("NewWorld with click on Cancel routes to WorldChooserMenu") {
      val event = performRouteAndClick(SceneRoute.NewWorld, (-0.1f, -0.8f))
      assertSceneChange(event, SceneRoute.WorldChooser)
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

      val event = performRouteAndClick(SceneRoute.NewWorld, (0.1f, -0.8f))
      assertSceneChange(event, SceneRoute.Game(saveDirPath.toFile, true, false, null))
    }
  }

  def testMultiplayerMenu(): Unit = {
    test("Multiplayer routes to MultiplayerMenu") {
      val scene = performSingleRoute(SceneRoute.Multiplayer)
      assert(scene.isInstanceOf[Menus.MultiplayerMenu])
    }

    test("Multiplayer with click on Join routes to JoinWorldChooserMenu") {
      val event = performRouteAndClick(SceneRoute.Multiplayer, (0, 0.2f))
      assertSceneChange(event, SceneRoute.JoinWorld)
    }

    test("Multiplayer with click on Host routes to HostWorldChooserMenu") {
      val event = performRouteAndClick(SceneRoute.Multiplayer, (0, -0.1f))
      assertSceneChange(event, SceneRoute.HostWorld)
    }

    test("Multiplayer with click on Back routes to MainMenu") {
      val event = performRouteAndClick(SceneRoute.Multiplayer, (0, -0.8f))
      assertSceneChange(event, SceneRoute.Main)
    }
  }

  def testJoinWorldChooserMenu(): Unit = {
    test("JoinWorld routes to JoinWorldChooserMenu") {
      val scene = performSingleRoute(SceneRoute.JoinWorld)
      assert(scene.isInstanceOf[Menus.JoinWorldChooserMenu])
    }

    test("JoinWorld with click on Back routes to MultiplayerMenu") {
      val event = performRouteAndClick(SceneRoute.JoinWorld, (-0.1f, -0.8f))
      assertSceneChange(event, SceneRoute.Multiplayer)
    }
  }

  def testHostWorldChooserMenu(): Unit = {
    test("HostWorld routes to HostWorldChooserMenu") {
      val scene = performSingleRoute(SceneRoute.HostWorld)
      assert(scene.isInstanceOf[Menus.HostWorldChooserMenu])
    }

    test("HostWorld with click on Back routes to MultiplayerMenu") {
      val event = performRouteAndClick(SceneRoute.HostWorld, (-0.1f, -0.8f))
      assertSceneChange(event, SceneRoute.Multiplayer)
    }
  }

  def testSettingsMenu(): Unit = {
    test("Settings routes to SettingsMenu") {
      val scene = performSingleRoute(SceneRoute.Settings)
      assert(scene.isInstanceOf[Menus.SettingsMenu])
    }

    test("Settings with click on Back routes to MainMenu") {
      val event = performRouteAndClick(SceneRoute.Settings, (0, -0.4f))
      assertSceneChange(event, SceneRoute.Main)
    }
  }

  def testGameScene(): Unit = {
    test("Game routes to GameScene".ignore) {
      val scene = performSingleRoute(SceneRoute.Game(saveDirPath.toFile, true, false, null))
      assert(scene.isInstanceOf[GameScene])
      scene.unload()
    }

    test("Game with Escape key and click on Back to menu routes to MainMenu".ignore) {
      val clickAt = (0f, -0.4f)

      val event = performRouteAndSendEvents(
        SceneRoute.Game(saveDirPath.toFile, true, false, null),
        Seq(
          Event.KeyEvent(KeyboardKey.Escape, 0, KeyAction.Press, KeyMods.none),
          Event.MouseClickEvent(MouseButton.Left, MouseAction.Press, KeyMods.none, clickAt),
          Event.MouseClickEvent(MouseButton.Left, MouseAction.Release, KeyMods.none, clickAt)
        ),
        FileSystem.createNull()
      )
      assertSceneChange(event, SceneRoute.Main)
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

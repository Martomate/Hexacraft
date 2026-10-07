package hexacraft.tool

import hexacraft.client.{BlockTextureLoader, GameClient, NetworkChannel}
import hexacraft.gui.Scene
import hexacraft.infra.audio.AudioSystem
import hexacraft.infra.fs.FileSystem
import hexacraft.infra.window.{CursorMode, WindowSystem}
import hexacraft.main.{GameScene, MainWindow, SceneRoute, SceneRouter}
import hexacraft.nbt.Nbt
import hexacraft.server.GameServer
import hexacraft.util.Channel
import hexacraft.world.*
import hexacraft.world.coord.CylCoords

import java.nio.file.Files
import java.util.UUID

object FarRenderExperiment {
  def main(args: Array[String]): Unit = {
    val saveDir = Files.createTempDirectory("hexacraft_world_")

    val fs = FileSystem.create()
    val windowSystem = WindowSystem.create()
    val audioSystem = AudioSystem.createNull()

    var running = true

    given cylSize: CylinderSize = CylinderSize(8)

    val worldProvider = FakeWorldProvider(1234)

    val playerId = UUID.randomUUID
    val player = Player.atStartPos(playerId, "Dude", CylCoords(0, 300, 0))
    player.flying = true
    player.rotation.x = 0.8 // look slightly down to see both the ground and the horizon
    worldProvider.savePlayerData(Nbt.encode(player), player.id)

    val server = GameServer.create(
      false,
      1298,
      WorldInfo(2, "test world", cylSize, WorldGenSettings.fromSeed(1234)),
      worldProvider,
      renderDistance = 10
    )

    new Thread(() => {
      ToolUtils.runAtSteadyFps(60)(running) {
        server.tick()
      }
      server.unload()
    }).start()

    new Thread(() => {
      val window = MainWindow(true, saveDir.toFile, fs, audioSystem, windowSystem)
      window.setNextScene(SceneRoute.Game(saveDir.resolve("test_world").toFile, true, false, null))

      val router = new SceneRouter {
        override def route(sceneRoute: SceneRoute): (Scene, Channel.Receiver[SceneRouter.Event]) = {
          val (tx, rx) = Channel[SceneRouter.Event]()
          val scene = sceneRoute match {
            case SceneRoute.Game(saveDir, isHosting, isOnline, _) =>
              require(isHosting)
              require(!isOnline)

              val channel = NetworkChannel.client("127.0.0.1", 1298)
              val (client, rx) = GameClient
                .create(
                  playerId,
                  "",
                  channel,
                  isOnline,
                  BlockTextureLoader.instance,
                  window.windowSize,
                  audioSystem,
                  maxChunksToLoad = 0, // the far distance renderer is not using this data, so let's not load it
                  renderDistance = 10,
                  useFarDistanceRenderer = true
                )
                .unwrap()

              rx.onEvent {
                case GameClient.Event.GameQuit =>
                  tx.send(SceneRouter.Event.QuitRequested)
                case GameClient.Event.CursorCaptured =>
                  window.setCursorMode(CursorMode.Disabled)
                case GameClient.Event.CursorReleased =>
                  window.setCursorMode(CursorMode.Normal)
              }

              new GameScene(client, None)
            case _ =>
              throw new IllegalStateException("missing route")
          }
          (scene, rx)
        }
      }

      try {
        window.run(router)
      } finally {
        running = false
      }
    }).start()

    while running do {
      windowSystem.performCallsAsMainThread()
      Thread.sleep(0, 10000)
    }
  }
}

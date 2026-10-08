package hexacraft.tool

import hexacraft.client.{BlockTextureLoader, GameClient, NetworkChannel}
import hexacraft.gui.Scene
import hexacraft.infra.audio.AudioSystem
import hexacraft.infra.fs.FileSystem
import hexacraft.infra.window.{CursorMode, WindowSystem}
import hexacraft.main.{GameScene, MainWindow, SceneRoute, SceneRouter}
import hexacraft.server.GameServer
import hexacraft.server.world.FakeWorldProvider
import hexacraft.util.Channel
import hexacraft.world.CylinderSize

import java.nio.file.Files
import java.util.UUID

/** Run one instance with `host [port]` and another with `join [port]`.
  *
  * The host creates a new (in-memory) world and hosts it on the given port, and the joining instance connects to it
  * on 127.0.0.1. This makes it possible to control one player while watching it from the other instance.
  */
object MultiplayerExperiment {
  private val DefaultPort = 1298

  def main(args: Array[String]): Unit = {
    val (isHosting, port) = args.toList match {
      case "host" :: rest => (true, rest.headOption.map(_.toInt).getOrElse(DefaultPort))
      case "join" :: rest => (false, rest.headOption.map(_.toInt).getOrElse(DefaultPort))
      case _ =>
        println("Usage: MultiplayerExperiment (host|join) [port]")
        return
    }

    val saveDir = Files.createTempDirectory("hexacraft_world_")

    val fs = FileSystem.create()
    val windowSystem = WindowSystem.create()
    val audioSystem = AudioSystem.createNull()

    var running = true

    if isHosting then {
      given CylinderSize = CylinderSize(8)

      val worldProvider = FakeWorldProvider(System.currentTimeMillis)
      val server = GameServer.create(true, port, worldProvider.worldInfo, worldProvider, renderDistance = 10)

      new Thread(() => {
        ToolUtils.runAtSteadyFps(60)(running) {
          server.tick()
        }
        server.unload()
      }).start()
    }

    val playerId = UUID.randomUUID
    val playerName = if isHosting then "Host" else "Guest"

    new Thread(() => {
      val window = MainWindow(true, saveDir.toFile, fs, audioSystem, windowSystem)
      window.setNextScene(SceneRoute.Game(null, isHosting, true, ("127.0.0.1", port)))

      val router = new SceneRouter {
        override def route(sceneRoute: SceneRoute): (Scene, Channel.Receiver[SceneRouter.Event]) = {
          val (tx, rx) = Channel[SceneRouter.Event]()
          val scene = sceneRoute match {
            case SceneRoute.Game(_, _, isOnline, (ip, port)) =>
              val channel = NetworkChannel.client(ip, port)
              val (client, rx) = GameClient
                .create(
                  playerId,
                  playerName,
                  channel,
                  isOnline,
                  BlockTextureLoader.instance,
                  window.windowSize,
                  audioSystem,
                  maxChunksToLoad = 5,
                  renderDistance = 10
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

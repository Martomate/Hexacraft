package hexacraft.tool

import hexacraft.client.{BlockTextureLoader, GameClient}
import hexacraft.gui.{RenderContext, Scene, TickContext}
import hexacraft.infra.audio.AudioSystem
import hexacraft.infra.fs.FileSystem
import hexacraft.infra.window.{CursorMode, WindowSystem}
import hexacraft.main.{MainWindow, SceneRoute, SceneRouter}
import hexacraft.nbt.Nbt
import hexacraft.server.GameServer
import hexacraft.util.Channel
import hexacraft.world.{CylinderSize, FakeWorldProvider, Player, WorldGenSettings, WorldInfo}
import hexacraft.world.coord.CylCoords

import java.nio.file.Files
import java.util.UUID

object TerrainRenderExperiment {
  def main(args: Array[String]): Unit = {
    val saveDir = Files.createTempDirectory("hexacraft_world_")

    val fs = FileSystem.create()
    val windowSystem = WindowSystem.create()
    val audioSystem = AudioSystem.createNull()

    var running = true

    val gameThread = new Thread(() => {
      val window = MainWindow(true, saveDir.toFile, fs, audioSystem, windowSystem)
      window.setNextScene(SceneRoute.Game(saveDir.resolve("test_world").toFile, true, false, null))

      val router = new SceneRouter {
        override def route(sceneRoute: SceneRoute): (Scene, Channel.Receiver[SceneRouter.Event]) = {
          val (tx, rx) = Channel[SceneRouter.Event]()
          val scene = sceneRoute match {
            case SceneRoute.Game(saveDir, isHosting, isOnline, _) =>
              require(isHosting)
              require(!isOnline)

              given cylSize: CylinderSize = CylinderSize(8)

              val worldProvider = FakeWorldProvider(1234)

              val playerId = UUID.randomUUID
              val player = Player.atStartPos(playerId, "Dude", CylCoords(0, 50, 0))
              player.flying = true
              player.rotation.x = 0.3 // look slightly down to see both the ground and the horizon
              worldProvider.savePlayerData(Nbt.encode(player), player.id)

              val server = GameServer.create(
                isOnline,
                1298,
                WorldInfo(2, "test world", cylSize, WorldGenSettings.fromSeed(1234)),
                worldProvider,
                renderDistance = 20,
                maxChunksToLoadPerTick = 20
              )

              val (client, rx) = GameClient
                .create(
                  playerId,
                  "",
                  "127.0.0.1",
                  1298,
                  isOnline,
                  BlockTextureLoader.instance,
                  window.windowSize,
                  audioSystem,
                  maxChunksToLoad = 50,
                  renderDistance = 20
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

              new Scene {
                override def handleEvent(event: hexacraft.gui.Event): Boolean = {
                  client.handleEvent(event)
                }

                override def windowFocusChanged(focused: Boolean): Unit = {
                  client.windowFocusChanged(focused)
                }

                override def windowResized(width: Int, height: Int): Unit = {
                  client.windowResized(width, height)
                }

                override def frameBufferResized(width: Int, height: Int): Unit = {
                  client.frameBufferResized(width, height)
                }

                override def render(context: RenderContext): Unit = {
                  client.render(context)
                }

                override def tick(ctx: TickContext): Unit = {
                  server.tick()
                  client.tick(ctx)
                }

                override def unload(): Unit = {
                  client.unload()
                  server.unload()
                }
              }
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
    })

    gameThread.start()

    while running do {
      windowSystem.performCallsAsMainThread()
      Thread.sleep(0, 10000)
    }
  }
}

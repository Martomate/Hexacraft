package hexacraft.main

import hexacraft.client.{BlockTextureLoader, GameClient}
import hexacraft.gui.{RenderContext, Scene, TickContext}
import hexacraft.infra.audio.AudioSystem
import hexacraft.infra.fs.FileSystem
import hexacraft.infra.window.{CursorMode, WindowSystem}
import hexacraft.server.{GameServer, WorldProviderFromFile}
import hexacraft.util.Channel
import hexacraft.world.{CylinderSize, WorldGenSettings, WorldInfo}

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

              val server = GameServer.create(
                isOnline,
                1298,
                WorldInfo(2, "test world", CylinderSize(8), WorldGenSettings.fromSeed(1234)),
                WorldProviderFromFile(saveDir, fs),
                renderDistance = 20
              )

              val (client, rx) = GameClient
                .create(
                  UUID.randomUUID,
                  "",
                  "127.0.0.1",
                  1298,
                  isOnline,
                  BlockTextureLoader.instance,
                  window.windowSize,
                  audioSystem,
                  maxChunksToLoad = 20,
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
      Thread.sleep(1)
    }
  }
}

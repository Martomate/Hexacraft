package hexacraft.main

import hexacraft.client.{BlockTextureLoader, GameClient, NetworkChannel}
import hexacraft.gui.*
import hexacraft.infra.audio.AudioSystem
import hexacraft.main.GameScene.Event.{CursorCaptured, CursorReleased, GameQuit}
import hexacraft.server.GameServer
import hexacraft.server.world.WorldProvider
import hexacraft.util.{Channel, Result, TickLoop}
import hexacraft.world.{CylinderSize, WorldInfo}

import java.util.UUID

object GameScene {
  enum Event {
    case GameQuit
    case CursorCaptured
    case CursorReleased
  }

  case class ClientParams(
      playerId: UUID,
      playerName: String,
      serverIp: String,
      serverPort: Int,
      isOnline: Boolean,
      textureLoader: BlockTextureLoader,
      audioSystem: AudioSystem,
      initialWindowSize: WindowSize
  )
  case class ServerParams(worldInfo: WorldInfo, worldProvider: WorldProvider)

  def create(
      c: ClientParams,
      serverParams: Option[ServerParams]
  ): Result[(GameScene, Channel.Receiver[GameScene.Event]), String] = {
    val (tx, rx) = Channel[GameScene.Event]()

    val maxChunksToLoad = 5
    val renderDistance = 8 * CylinderSize.y60

    val server = serverParams.map { s =>
      val server = GameServer.create(
        c.isOnline,
        c.serverPort,
        s.worldInfo,
        s.worldProvider,
        renderDistance
      )

      // The server runs on its own thread so the client can wait for the server without causing a deadlock
      val tickLoop = TickLoop.start("server-tick", 60)(() => server.tick())

      LocalServer(server, tickLoop)
    }

    val client =
      try {
        val channel = NetworkChannel.client(c.serverIp, c.serverPort)
        val (client, clientEvents) = GameClient.create(
          c.playerId,
          c.playerName,
          channel,
          c.isOnline,
          c.textureLoader,
          c.initialWindowSize,
          c.audioSystem,
          maxChunksToLoad,
          renderDistance
        ) match {
          case Result.Ok(res) => res
          case Result.Err(message) =>
            server.foreach(_.unload())
            return Result.Err(s"failed to start game: $message")
        }
        clientEvents.onEvent {
          case GameClient.Event.GameQuit       => tx.send(GameQuit)
          case GameClient.Event.CursorCaptured => tx.send(CursorCaptured)
          case GameClient.Event.CursorReleased => tx.send(CursorReleased)
        }
        client
      } catch {
        case e: Exception =>
          server.foreach(_.unload())
          throw e
      }

    Result.Ok((new GameScene(client, server), rx))
  }
}

/** A server that is run by this game (i.e. not a remote server) */
class LocalServer(server: GameServer, tickLoop: TickLoop) {
  def unload(): Unit = {
    tickLoop.stop()
    server.unload()
  }
}

class GameScene(val client: GameClient, server: Option[LocalServer]) extends Scene {
  override def handleEvent(event: Event): Boolean = {
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
    client.tick(ctx)
  }

  override def unload(): Unit = {
    client.unload() // the server is still running so the client can log out

    server.foreach(_.unload())
  }
}

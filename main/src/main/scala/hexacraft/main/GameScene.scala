package hexacraft.main

import hexacraft.client.{BlockTextureLoader, GameClient}
import hexacraft.game.GameKeyboard
import hexacraft.gui.*
import hexacraft.infra.audio.AudioSystem
import hexacraft.main.GameScene.Event.{CursorCaptured, CursorReleased, GameQuit}
import hexacraft.server.GameServer
import hexacraft.util.{Channel, Result}
import hexacraft.world.WorldProvider

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
      keyboard: GameKeyboard,
      textureLoader: BlockTextureLoader,
      audioSystem: AudioSystem,
      initialWindowSize: WindowSize
  )
  case class ServerParams(worldProvider: WorldProvider)

  def create(
      c: ClientParams,
      serverParams: Option[ServerParams]
  ): Result[(GameScene, Channel.Receiver[GameScene.Event]), String] = {
    val (tx, rx) = Channel[GameScene.Event]()

    val server = serverParams.map(s => GameServer.create(c.isOnline, c.serverPort, s.worldProvider))

    val (client, clientEvents) = GameClient.create(
      c.playerId,
      c.playerName,
      c.serverIp,
      c.serverPort,
      c.isOnline,
      c.keyboard,
      c.textureLoader,
      c.initialWindowSize,
      c.audioSystem
    ) match {
      case Result.Ok(res) => res
      case Result.Err(message) =>
        if server.isDefined then {
          server.get.unload()
        }
        return Result.Err(s"failed to start game: $message")
    }

    clientEvents.onEvent {
      case GameClient.Event.GameQuit       => tx.send(GameQuit)
      case GameClient.Event.CursorCaptured => tx.send(CursorCaptured)
      case GameClient.Event.CursorReleased => tx.send(CursorReleased)
    }

    Result.Ok((new GameScene(client, server), rx))
  }
}

class GameScene private (val client: GameClient, server: Option[GameServer]) extends Scene {
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
    if server.isDefined then {
      server.get.tick()
    }
    client.tick(ctx)
  }

  override def unload(): Unit = {
    client.unload()

    if server.isDefined then {
      server.get.unload()
    }
  }
}

package hexacraft.game

import hexacraft.game.GameScene.Event.{CursorCaptured, CursorReleased, GameQuit}
import hexacraft.gui.*
import hexacraft.gui.comp.GUITransformation
import hexacraft.infra.audio.AudioSystem
import hexacraft.util.Channel
import hexacraft.world.*

object GameScene {
  enum Event {
    case GameQuit
    case CursorCaptured
    case CursorReleased
  }

  def createHostedGame(
      serverIp: String,
      serverPort: Int,
      isOnline: Boolean,
      worldInfo: WorldInfo,
      keyboard: GameKeyboard,
      blockLoader: BlockTextureLoader,
      initialWindowSize: WindowSize,
      audioSystem: AudioSystem,
      worldProvider: WorldProvider
  ): (GameScene, Channel.Receiver[GameScene.Event]) = {
    val (tx, rx) = Channel[GameScene.Event]()

    val server = GameServer.create(isOnline, worldProvider)

    val client: GameClient = createClient(serverIp, serverPort, isOnline, keyboard, audioSystem, initialWindowSize, tx)

    (new GameScene(client, Some(server)), rx)
  }

  def createRemoteGame(
      serverIp: String,
      serverPort: Int,
      isOnline: Boolean,
      worldInfo: WorldInfo,
      keyboard: GameKeyboard,
      blockLoader: BlockTextureLoader,
      initialWindowSize: WindowSize,
      audioSystem: AudioSystem
  ): (GameScene, Channel.Receiver[GameScene.Event]) = {
    val (tx, rx) = Channel[Event]()

    val client: GameClient = createClient(serverIp, serverPort, isOnline, keyboard, audioSystem, initialWindowSize, tx)

    (new GameScene(client, None), rx)
  }

  private def createClient(
      serverIp: String,
      serverPort: Int,
      isOnline: Boolean,
      keyboard: GameKeyboard,
      audioSystem: AudioSystem,
      initialWindowSize: WindowSize,
      tx: Channel.Sender[Event]
  ): GameClient = {
    val (client, clientEvents) = GameClient.create(
      serverIp,
      serverPort,
      isOnline,
      keyboard,
      BlockTextureLoader.instance,
      initialWindowSize,
      audioSystem
    )

    clientEvents.onEvent {
      case GameClient.Event.GameQuit       => tx.send(GameQuit)
      case GameClient.Event.CursorCaptured => tx.send(CursorCaptured)
      case GameClient.Event.CursorReleased => tx.send(CursorReleased)
    }

    client
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

  override def render(transformation: GUITransformation)(using RenderContext): Unit = {
    client.render(transformation)
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

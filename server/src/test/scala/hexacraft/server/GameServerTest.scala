package hexacraft.server

import hexacraft.game.NetworkPacket
import hexacraft.nbt.Nbt
import hexacraft.server.GameServerTest.randomPort
import hexacraft.world.{CylinderSize, FakeWorldProvider, WorldProvider}

import munit.FunSuite
import org.zeromq.{SocketType, ZContext}
import org.zeromq.ZMQ.Socket

import java.util.UUID
import scala.util.Random

object GameServerTest {
  private var _nextPort = 1234
  def randomPort(): Int = GameServerTest.synchronized {
    val port = _nextPort
    _nextPort += 1
    port
  }
}

class GameServerTest extends FunSuite {
  given CylinderSize = CylinderSize(8)

  private def makeClientSocket(context: ZContext): Socket = {
    val socket = context.createSocket(SocketType.DEALER)

    val clientId = (new Random().nextInt(1000000) + 1000000).toString

    socket.setIdentity(clientId.getBytes)
    socket.setSendTimeOut(3000)
    socket.setReceiveTimeOut(3000)
    socket.setReconnectIVL(-1)
    socket.setHeartbeatIvl(200)
    socket.setHeartbeatTimeout(1000)

    socket
  }

  class SimpleSocket(socket: Socket) {
    @throws[RuntimeException]
    def send(packet: NetworkPacket): Unit = {
      if !socket.send(packet.serialize()) then {
        throw new RuntimeException(s"Could not send message: ${socket.errno()}")
      }
    }

    @throws[RuntimeException]
    def receive(): Nbt = {
      val res = socket.recv(0)
      if res == null then {
        throw new RuntimeException(s"Could not receive message: ${socket.errno()}")
      }
      Nbt.fromBinary(res)._2
    }
  }

  class Session(context: ZContext, val server: GameServer, val port: Int) {
    def connect(): SimpleSocket = {
      val socket = makeClientSocket(context)
      socket.connect(s"tcp://localhost:$port")
      SimpleSocket(socket)
    }
  }

  private def runServer(worldProvider: WorldProvider)(useServer: Session => Unit): Unit = {
    val port = randomPort()

    val server = GameServer.create(true, port, worldProvider)
    val context = ZContext()

    try {
      useServer(Session(context, server, port))
    } finally {
      context.close()
      server.unload()
    }
  }

  test("anyone can fetch world info") {
    val seed = 9876

    runServer(FakeWorldProvider(seed)) { s =>
      s.server.tick()

      val socket = s.connect()

      socket.send(NetworkPacket.GetWorldInfo)

      val tag = socket.receive()

      assertEquals(
        tag.asMap.get,
        Nbt.makeMap(
          "version" -> Nbt.ShortTag(1.toShort),
          "general" -> Nbt.makeMap(
            "worldSize" -> Nbt.ByteTag(8.toByte),
            "name" -> Nbt.StringTag("test world") // TODO: should this be the default?
          ),
          "gen" -> Nbt.makeMap(
            "seed" -> Nbt.LongTag(seed),
            "blockGenScale" -> Nbt.DoubleTag(0.1),
            "heightMapGenScale" -> Nbt.DoubleTag(0.01),
            "blockDensityGenScale" -> Nbt.DoubleTag(0.01),
            "biomeHeightGenScale" -> Nbt.DoubleTag(0.001),
            "biomeHeightVariationGenScale" -> Nbt.DoubleTag(0.001)
          )
        )
      )

      socket.send(NetworkPacket.Logout)
    }
  }

  test("client can login") {
    val seed = 9876

    runServer(FakeWorldProvider(seed)) { s =>
      s.server.tick()

      val socket = s.connect()

      val playerId = UUID.randomUUID()
      val playerName = "The Dude"

      socket.send(NetworkPacket.Login(playerId, playerName))

      val tag = socket.receive()

      assertEquals(
        tag.asMap.get,
        Nbt.makeMap(
          "success" -> Nbt.ByteTag(true)
        )
      )

      socket.send(NetworkPacket.Logout)
      Thread.sleep(1) // this gives the GameServer enough time to remove the player, which prevents the sleep in unload
    }
  }

  test("first client gets server start message after login") {
    val seed = 9876

    runServer(FakeWorldProvider(seed)) { s =>
      s.server.tick()

      val socket = s.connect()

      val playerId = UUID.randomUUID()
      val playerName = "The Dude"

      socket.send(NetworkPacket.Login(playerId, playerName))

      socket.receive()

      socket.send(NetworkPacket.GetEvents)

      val tag = socket.receive()

      val messages = tag.asMap.get.getList("messages").get
      assertEquals(messages.size, 1)

      val firstMessage = messages.head.asMap.get
      val messageText = firstMessage.getString("text").get
      assert(messageText.matches(s"Server started on (.*):${s.port}"), messageText)

      socket.send(NetworkPacket.Logout)
    }
  }

  test("server sends login notification to existing players") {
    val seed = 9876

    runServer(FakeWorldProvider(seed)) { s =>
      s.server.tick()

      val socket1 = s.connect()

      val player1Id = UUID.randomUUID()
      val player1Name = "The Dude"

      socket1.send(NetworkPacket.Login(player1Id, player1Name))

      socket1.receive()

      socket1.send(NetworkPacket.GetEvents)

      socket1.receive() // get the events now so we only get new events next time

      val socket2 = s.connect()

      val player2Id = UUID.randomUUID()
      val player2Name = "The Dude"

      socket2.send(NetworkPacket.Login(player2Id, player2Name))

      socket2.receive()

      {
        socket1.send(NetworkPacket.GetEvents)

        val tag = socket1.receive()

        val messages = tag.asMap.get.getList("messages").get
        assertEquals(messages.size, 1)

        assertEquals(
          messages.head.asMap.get,
          Nbt.makeMap(
            "text" -> Nbt.StringTag(s"$player2Name logged in"),
            "sender" -> Nbt.makeMap("kind" -> Nbt.StringTag("server"))
          )
        )
      }

      socket2.send(NetworkPacket.Logout)

      {
        socket1.send(NetworkPacket.GetEvents)

        val tag = socket1.receive()

        val messages = tag.asMap.get.getList("messages").get
        assertEquals(messages.size, 1)

        assertEquals(
          messages.head.asMap.get,
          Nbt.makeMap(
            "text" -> Nbt.StringTag(s"$player2Name logged out"),
            "sender" -> Nbt.makeMap("kind" -> Nbt.StringTag("server"))
          )
        )
      }

      socket1.send(NetworkPacket.Logout)
    }
  }
}

package hexacraft.server

import hexacraft.game.NetworkPacket
import hexacraft.nbt.Nbt
import hexacraft.rs.RustLib
import hexacraft.server.GameServerTest.randomPort
import hexacraft.server.entity.PlayerEntityModel
import hexacraft.server.world.FakeWorldProvider
import hexacraft.util.TickLoop
import hexacraft.world.{CylinderSize, EntityEvent}
import hexacraft.world.entity.EntityModel

import munit.FunSuite

import java.io.Closeable
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

  class SimpleSocket(socketHandle: Long) extends Closeable {
    @throws[RuntimeException]
    def send(packet: NetworkPacket): Unit = {
      RustLib.ClientSocket.send(socketHandle, packet.serialize())
    }

    @throws[RuntimeException]
    def receive(): Nbt = {
      while true do {
        tryReceive() match {
          case None =>
            Thread.sleep(1)
          case Some(res) =>
            return res
        }
      }
      null
    }

    @throws[RuntimeException]
    def tryReceive(): Option[Nbt] = {
      Option(RustLib.ClientSocket.tryReceive(socketHandle)).map(res => Nbt.fromBinary(res)._2)
    }

    override def close(): Unit = {
      RustLib.ClientSocket.close(socketHandle)
    }
  }

  class Session(val server: GameServer, val port: Int) {
    def connect(): SimpleSocket = {
      val clientId = (new Random().nextInt(1000000) + 1000000).toString
      val socketHandle = RustLib.ClientSocket.create(clientId.getBytes())
      RustLib.ClientSocket.connect(socketHandle, "localhost", port)
      SimpleSocket(socketHandle)
    }
  }

  /** Runs the server and ticks it continuously (unless `ticking` is false, in which case the test has to tick it) */
  private def runServer(worldProvider: FakeWorldProvider, ticking: Boolean = true)(useServer: Session => Unit): Unit = {
    val port = randomPort()

    val server = GameServer.create(true, port, worldProvider.worldInfo, worldProvider, 10)
    val tickLoop = Option.when(ticking)(TickLoop.start("test-server-tick", 60)(() => server.tick()))

    try {
      useServer(Session(server, port))
    } finally {
      tickLoop.foreach(_.stop())
      server.unload()
    }
  }

  test("packets are handled when the server ticks") {
    runServer(FakeWorldProvider(9876), ticking = false) { s =>
      val socket = s.connect()
      socket.send(NetworkPacket.GetWorldInfo)

      Thread.sleep(50)
      assertEquals(socket.tryReceive(), None)

      s.server.tick()
      assert(socket.receive().asMap.get.getMap("general").isDefined)
    }
  }

  test("anyone can fetch world info") {
    val seed = 9876

    runServer(FakeWorldProvider(seed)) { s =>
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
            "biomeHeightVariationGenScale" -> Nbt.DoubleTag(0.001),
            "generateOceans" -> Nbt.ByteTag(false)
          )
        )
      )

      socket.send(NetworkPacket.Logout)
    }
  }

  test("client can login") {
    val seed = 9876

    runServer(FakeWorldProvider(seed)) { s =>
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
      Thread.sleep(10)

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

  test("server sends the entity of a new player to existing players, and they can fetch its model") {
    runServer(FakeWorldProvider(9876)) { s =>
      val socket1 = s.connect()
      socket1.send(NetworkPacket.Login(UUID.randomUUID(), "Player 1"))
      socket1.receive()

      socket1.send(NetworkPacket.GetEvents)
      socket1.receive() // get the events now so we only get new events next time

      val socket2 = s.connect()
      val player2Id = UUID.randomUUID()
      socket2.send(NetworkPacket.Login(player2Id, "Player 2"))
      socket2.receive()

      socket1.send(NetworkPacket.GetEvents)
      val entityEvents = socket1.receive().asMap.get.getMap("entity_events").get

      val ids = entityEvents.getList("ids").get.map {
        case Nbt.StringTag(id) => UUID.fromString(id)
        case t                 => fail(s"Expected an entity id, but got $t")
      }
      val events = entityEvents.getList("events").get.map(e => Nbt.decode[EntityEvent](e.asMap.get).get)

      val spawnEvents = ids.zip(events).collect { case (`player2Id`, e: EntityEvent.Spawned) => e }
      assertEquals(spawnEvents.size, 1)

      val modelId = spawnEvents.head.modelId.get

      socket1.send(NetworkPacket.GetModels(Seq(modelId, "unknown model")))
      val models = socket1.receive().asMap.get.getMap("models").get

      assertEquals(models.vs.keySet, Set(modelId)) // unknown models are left out
      assertEquals(models.getMap(modelId).flatMap(Nbt.decode[EntityModel]), Some(PlayerEntityModel.model))

      socket2.send(NetworkPacket.Logout)
      socket1.send(NetworkPacket.Logout)
    }
  }
}

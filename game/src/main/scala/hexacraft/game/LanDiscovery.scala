package hexacraft.game

import java.net.*
import java.nio.charset.StandardCharsets
import java.util.UUID
import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.util.Try

/** Lets clients find servers on the local network without knowing their address.
  *
  * A hosting server periodically sends a small UDP datagram to a multicast group on a well-known port. Clients that
  * want to find servers join the group and listen for these announcements. The address of the server is taken from the
  * sender of the datagram, so only the game port has to be included in the announcement.
  */
object LanDiscovery {

  /** An administratively scoped (i.e. local) multicast address */
  private val GroupAddress: InetAddress = InetAddress.getByName("239.255.72.67")
  private val Port: Int = 44767

  private val Magic = "HEXACRAFT"
  private val ProtocolVersion = 1
  private val MaxPacketSize = 512

  /** @param serverId identifies the server instance, so that the same server is only listed once even if the
    *                 announcement arrives through several network interfaces
    */
  case class Announcement(serverId: UUID, port: Int, worldName: String)

  def encode(announcement: Announcement): Array[Byte] = {
    val Announcement(serverId, port, worldName) = announcement
    val bytes = s"$Magic;$ProtocolVersion;$serverId;$port;$worldName".getBytes(StandardCharsets.UTF_8)
    bytes.take(MaxPacketSize)
  }

  def decode(bytes: Array[Byte]): Option[Announcement] = {
    val parts = String(bytes, StandardCharsets.UTF_8).split(";", 5)
    if parts.length != 5 || parts(0) != Magic || parts(1) != ProtocolVersion.toString then {
      return None
    }
    for {
      serverId <- Try(UUID.fromString(parts(2))).toOption
      port <- parts(3).toIntOption.filter(p => p > 0 && p <= 65535)
    } yield Announcement(serverId, port, parts(4))
  }

  /** The network interfaces that multicast messages should be sent on and received from */
  private def multicastInterfaces(): Seq[NetworkInterface] = {
    val all = Try(NetworkInterface.networkInterfaces().iterator().asScala.toSeq).getOrElse(Seq.empty)

    // Loopback is included so that several listeners on the same machine can find the server. Duplicates are removed
    // by the listener.
    all.filter { i =>
      Try(i.isUp && i.supportsMulticast && !i.isVirtual).getOrElse(false) &&
      i.getInetAddresses.asScala.exists(_.isInstanceOf[Inet4Address])
    }
  }

  object Announcer {
    def start(gamePort: Int, worldName: String, intervalMillis: Long = 1500): Announcer = {
      val announcement = Announcement(UUID.randomUUID(), gamePort, worldName)
      new Announcer(encode(announcement), intervalMillis)
    }
  }

  /** Repeatedly announces a server on the local network until it is closed */
  class Announcer private (payload: Array[Byte], intervalMillis: Long) {
    @volatile private var running = true

    private val thread = Thread(() => run(), "lan-announcer")
    thread.setDaemon(true)
    thread.start()

    private def run(): Unit = {
      val socket = new MulticastSocket()
      try {
        socket.setTimeToLive(1) // stay on the local network
        val packet = new DatagramPacket(payload, payload.length, GroupAddress, Port)

        var interfaces = multicastInterfaces()
        val failingInterfaces = mutable.Set.empty[String]
        var lastInterfaceRefresh = System.currentTimeMillis()

        while running do {
          // Interfaces may come and go (e.g. when connecting to Wi-Fi)
          if System.currentTimeMillis() - lastInterfaceRefresh > 10_000 then {
            interfaces = multicastInterfaces()
            lastInterfaceRefresh = System.currentTimeMillis()
          }

          for i <- interfaces do {
            try {
              socket.setNetworkInterface(i)
              socket.send(packet)
              failingInterfaces -= i.getName
            } catch {
              case e: Exception =>
                // the interface might be down or not allowed to send, so just skip it (but only report it once)
                if failingInterfaces.add(i.getName) then {
                  println(s"Could not announce server on network interface ${i.getName}: ${e.getMessage}")
                }
            }
          }

          try {
            Thread.sleep(intervalMillis)
          } catch {
            case _: InterruptedException =>
          }
        }
      } finally {
        socket.close()
      }
    }

    def close(): Unit = {
      running = false
      thread.interrupt()
      thread.join()
    }
  }

  case class DiscoveredServer(address: String, port: Int, worldName: String)

  object Listener {

    /** Starts listening for servers. If the socket cannot be created no servers will be found. */
    def start(expiryMillis: Long = 5000): Listener = {
      val socket =
        try {
          Some(createSocket())
        } catch {
          case e: Exception =>
            println(s"Could not listen for LAN servers: ${e.getMessage}")
            None
        }
      new Listener(socket, expiryMillis)
    }

    private def createSocket(): MulticastSocket = {
      val socket = new MulticastSocket(null: SocketAddress) // unbound, so options can be set before binding
      try {
        socket.setReuseAddress(true)
        if socket.supportedOptions().contains(StandardSocketOptions.SO_REUSEPORT) then {
          // needed on some platforms for several game instances to listen at the same time
          socket.setOption(StandardSocketOptions.SO_REUSEPORT, true)
        }
        socket.bind(new InetSocketAddress(Port))

        val group = new InetSocketAddress(GroupAddress, 0)
        val joined = multicastInterfaces().count(i => Try(socket.joinGroup(group, i)).isSuccess)
        if joined == 0 then {
          socket.joinGroup(group, null) // let the OS pick an interface
        }
        socket
      } catch {
        case e: Exception =>
          socket.close()
          throw e
      }
    }
  }

  /** Keeps track of the servers that have recently announced themselves */
  class Listener private (socket: Option[MulticastSocket], expiryMillis: Long) {
    private case class Entry(server: DiscoveredServer, lastSeen: Long)

    private val entries: mutable.LinkedHashMap[UUID, Entry] = mutable.LinkedHashMap.empty

    private val thread = socket.map { s =>
      val t = Thread(() => run(s), "lan-listener")
      t.setDaemon(true)
      t.start()
      t
    }

    private def run(socket: MulticastSocket): Unit = {
      val buffer = new Array[Byte](MaxPacketSize)
      while !socket.isClosed do {
        try {
          val packet = new DatagramPacket(buffer, buffer.length)
          socket.receive(packet)
          for a <- decode(packet.getData.slice(packet.getOffset, packet.getOffset + packet.getLength)) do {
            val server = DiscoveredServer(packet.getAddress.getHostAddress, a.port, a.worldName)
            entries.synchronized {
              // The same announcement can arrive both through loopback and a real network interface.
              // Prefer the non-loopback address, so the listed address doesn't keep changing.
              val keepOldAddress = packet.getAddress.isLoopbackAddress && entries.contains(a.serverId)
              val newServer = if keepOldAddress then entries(a.serverId).server else server
              entries(a.serverId) = Entry(newServer, System.currentTimeMillis())
            }
          }
        } catch {
          case _: SocketException => // the socket was closed
        }
      }
    }

    /** The servers that have announced themselves recently, in the order they were first seen */
    def servers: Seq[DiscoveredServer] = {
      val now = System.currentTimeMillis()
      entries.synchronized {
        entries.filterInPlace((_, e) => now - e.lastSeen <= expiryMillis)
        entries.values.map(_.server).toSeq
      }
    }

    def close(): Unit = {
      socket.foreach(_.close())
      thread.foreach(_.join())
    }
  }
}

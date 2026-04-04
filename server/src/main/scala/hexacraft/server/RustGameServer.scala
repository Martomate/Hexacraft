package hexacraft.server

import hexacraft.rs.RustLib

import java.nio.file.Path

object RustGameServer {
  def start(isOnline: Boolean, port: Int, path: Path): RustGameServer = {
    val handle = RustLib.GameServer.start(isOnline, port, path.toAbsolutePath.toString)
    new RustGameServer(handle)
  }
}

class RustGameServer(handle: Long) {
  def stop(): Unit = RustLib.GameServer.stop(handle)
}

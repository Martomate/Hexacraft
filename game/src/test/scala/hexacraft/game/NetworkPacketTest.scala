package hexacraft.game

import munit.FunSuite

class NetworkPacketTest extends FunSuite {
  test("GetModels survives a round trip through serialization") {
    val packet = NetworkPacket.GetModels(Seq("0123456789abcdef", "fedcba9876543210"))
    assertEquals(NetworkPacket.deserialize(packet.serialize()), packet)
  }

  test("GetModels without IDs survives a round trip through serialization") {
    val packet = NetworkPacket.GetModels(Seq())
    assertEquals(NetworkPacket.deserialize(packet.serialize()), packet)
  }
}

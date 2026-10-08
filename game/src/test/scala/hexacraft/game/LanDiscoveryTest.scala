package hexacraft.game

import hexacraft.game.LanDiscovery.Announcement

import munit.FunSuite

import java.nio.charset.StandardCharsets
import java.util.UUID

class LanDiscoveryTest extends FunSuite {
  test("an announcement can be encoded") {
    val uuid = UUID.randomUUID()
    val announcement = Announcement(uuid, 1234, "My world; with semicolons")
    assertEquals(
      String(LanDiscovery.encode(announcement), StandardCharsets.UTF_8),
      s"HEXACRAFT;1;$uuid;1234;My world; with semicolons"
    )
  }

  test("an announcement can be decoded after being encoded") {
    val announcement = Announcement(UUID.randomUUID(), 1234, "My world; with semicolons")
    assertEquals(LanDiscovery.decode(LanDiscovery.encode(announcement)), Some(announcement))
  }

  test("decode rejects unrelated data") {
    assertEquals(LanDiscovery.decode("hello".getBytes(StandardCharsets.UTF_8)), None)
    assertEquals(LanDiscovery.decode(Array.emptyByteArray), None)
  }

  test("decode rejects other protocol versions") {
    val bytes = s"HEXACRAFT;999;${UUID.randomUUID()};1234;world".getBytes(StandardCharsets.UTF_8)
    assertEquals(LanDiscovery.decode(bytes), None)
  }

  test("decode rejects invalid ports") {
    for port <- Seq("0", "70000", "abc") do {
      val bytes = s"HEXACRAFT;1;${UUID.randomUUID()};$port;world".getBytes(StandardCharsets.UTF_8)
      assertEquals(LanDiscovery.decode(bytes), None)
    }
  }

  test("a listener finds an announced server") {
    val listener = LanDiscovery.Listener.start()
    val announcer = LanDiscovery.Announcer.start(23456, "Test world", intervalMillis = 50)
    try {
      val deadline = System.currentTimeMillis() + 3000
      while !listener.servers.exists(_.port == 23456) && System.currentTimeMillis() < deadline do {
        Thread.sleep(20)
      }
      val found = listener.servers.filter(_.port == 23456)
      assertEquals(found.map(_.worldName), Seq("Test world"))
    } finally {
      announcer.close()
      listener.close()
    }
  }

  test("a listener forgets servers that stop announcing") {
    val listener = LanDiscovery.Listener.start(expiryMillis = 200)
    val announcer = LanDiscovery.Announcer.start(23457, "Test world", intervalMillis = 50)
    try {
      val deadline = System.currentTimeMillis() + 3000
      while !listener.servers.exists(_.port == 23457) && System.currentTimeMillis() < deadline do {
        Thread.sleep(20)
      }
      assert(listener.servers.exists(_.port == 23457))

      announcer.close()
      Thread.sleep(400)
      assert(!listener.servers.exists(_.port == 23457))
    } finally {
      listener.close()
    }
  }
}

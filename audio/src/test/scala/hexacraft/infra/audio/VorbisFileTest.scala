package hexacraft.infra.audio

import hexacraft.util.Tracker

import munit.FunSuite

class VorbisFileTest extends FunSuite {
  test("load ogg file") {
    val buf = VorbisFile.bundled("example_sound.ogg").samples

    assertEquals(buf.limit(), 11040)

    // these act as a regression test
    assertEquals(buf.get(0), -9.toShort)
    assertEquals(buf.get(123), -6.toShort)
    assertEquals(buf.get(buf.limit() / 2), 886.toShort)
    assertEquals(buf.get(buf.limit() - 1), 0.toShort)
  }
}

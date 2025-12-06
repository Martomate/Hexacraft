package hexacraft.infra.audio

import hexacraft.infra.fs.Bundle

import munit.FunSuite

class VorbisDecoderTest extends FunSuite {
  test("decode ogg file") {
    val SoundBuffer.Mono16(buf, sampleRate) =
      VorbisDecoder.decode(Bundle.locate("example_sound.ogg").get.readBytes()): @unchecked

    assertEquals(sampleRate, 44100)
    assertEquals(buf.limit(), 11040)

    // these act as a regression test
    assertEquals(buf.get(0), -9.toShort)
    assertEquals(buf.get(123), -6.toShort)
    assertEquals(buf.get(buf.limit() / 2), 886.toShort)
    assertEquals(buf.get(buf.limit() - 1), 0.toShort)
  }
}

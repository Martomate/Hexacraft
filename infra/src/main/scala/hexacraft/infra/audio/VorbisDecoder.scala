package hexacraft.infra.audio

import hexacraft.rs.RustLib

import org.lwjgl.BufferUtils

object VorbisDecoder {
  def decode(bytes: Array[Byte]): SoundBuffer = {
    val decoderHandle = RustLib.VorbisDecoder.decode(bytes)
    val sampleRate = RustLib.VorbisDecoder.getSampleRate(decoderHandle)
    val samples = RustLib.VorbisDecoder.getSamples(decoderHandle)
    RustLib.VorbisDecoder.destroy(decoderHandle)

    val buf = BufferUtils.createShortBuffer(samples.length)
    buf.put(samples)
    buf.flip()

    SoundBuffer.Mono16(buf, sampleRate)
  }
}

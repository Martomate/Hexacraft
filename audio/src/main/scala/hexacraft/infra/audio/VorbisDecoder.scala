package hexacraft.infra.audio

import org.lwjgl.stb.{STBVorbis, STBVorbisInfo}
import org.lwjgl.system.{MemoryStack, MemoryUtil}

import java.nio.ByteBuffer

object VorbisDecoder {
  def decode(bytes: Array[Byte]): SoundBuffer = {
    val data = ByteBuffer.allocateDirect(bytes.length)
    data.put(bytes)
    data.flip()

    val stack = MemoryStack.stackPush
    try {
      val error = stack.mallocInt(1)
      val decoder = STBVorbis.stb_vorbis_open_memory(data, error, null)
      if decoder == 0L then {
        throw new RuntimeException("Failed to open Ogg Vorbis file. Error: " + error.get(0))
      }
      val info = STBVorbisInfo.create()
      STBVorbis.stb_vorbis_get_info(decoder, info)
      val channels = info.channels
      val lengthSamples = STBVorbis.stb_vorbis_stream_length_in_samples(decoder)
      val pcm = MemoryUtil.memAllocShort(lengthSamples)
      pcm.limit(STBVorbis.stb_vorbis_get_samples_short_interleaved(decoder, channels, pcm) * channels)
      STBVorbis.stb_vorbis_close(decoder)
      SoundBuffer.Mono16(pcm, info.sample_rate())
    } finally {
      if stack != null then {
        stack.close()
      }
    }
  }
}

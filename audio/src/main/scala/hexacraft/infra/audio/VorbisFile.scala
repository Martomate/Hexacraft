package hexacraft.infra.audio

import hexacraft.infra.audio.AudioSystem.BufferId
import hexacraft.infra.fs.Bundle

import org.lwjgl.stb.{STBVorbis, STBVorbisInfo}
import org.lwjgl.system.{MemoryStack, MemoryUtil}

import java.nio.{ByteBuffer, ShortBuffer}

class VorbisFile(bytes: Array[Byte]) {
  def samples: ShortBuffer = {
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
      pcm
    } finally {
      if stack != null then {
        stack.close()
      }
    }
  }

  def load(audioSystem: AudioSystem): BufferId = {
    audioSystem.loadSoundBufferMono16(this.samples, 44100)
  }
}

object VorbisFile {
  def bundled(fileName: String): VorbisFile = {
    val bytes = Bundle.locate(fileName).get.readBytes()

    new VorbisFile(bytes)
  }
}
